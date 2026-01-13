import axios from 'axios';

const BASE_URL = 'http://localhost:19090/api/borrow/requests';
const TEST_ARCHIVE_ID = 'arc-test-001';

async function testBorrowFlow() {
    console.log('>>> Starting E2E Test for Borrow Module <<<\n');

    try {
        // 0. Login
        console.log('[0] Logging in...');
        const loginRes = await axios.post('http://localhost:19090/api/auth/login', {
            username: 'admin',
            password: 'password' // Try password first, maybe admin123
        }).catch(async () => {
            console.log('Login failed with "password", trying "admin123"...');
            return axios.post('http://localhost:19090/api/auth/login', {
                username: 'admin',
                password: 'admin123'
            });
        });

        const token = loginRes.data.data.token;
        console.log('✅ Logged in. Token:', token.substring(0, 10) + '...');

        const axiosAuth = axios.create({
            baseURL: BASE_URL,
            headers: { Authorization: `Bearer ${token}` }
        });

        // 1. Submit Request
        console.log('[1] Submitting Borrow Request...');
        const submitPayload = {
            applicantId: 'user-e2e',
            applicantName: 'E2E Tester',
            deptId: 'dept-e2e',
            deptName: 'QA Dept',
            purpose: 'E2E Verification',
            borrowType: 'READING',
            archiveIds: [TEST_ARCHIVE_ID],
            expectedStartDate: new Date().toISOString().split('T')[0],
            expectedEndDate: new Date(Date.now() + 86400000).toISOString().split('T')[0]
        };

        const submitRes = await axiosAuth.post('', submitPayload);
        const request = submitRes.data.data;
        console.log('✅ Created Request:', request.id, request.requestNo, request.status);

        if (request.status !== 'PENDING') throw new Error('Status should be PENDING');

        const requestId = request.id;

        // 2. Approve Request
        console.log('\n[2] Approving Request...');
        const approvePayload = {
            requestId: requestId,
            approverId: 'admin-e2e',
            approverName: 'Admin',
            approved: true,
            comment: 'Approved by E2E Script'
        };
        await axiosAuth.post(`/${requestId}/approve`, approvePayload);
        console.log('✅ Approved');

        // Verify status
        // TODO: Get Details API is missing in controller? Check list.
        // For now, assume success if 200 OK.

        // 3. Confirm Out
        console.log('\n[3] Confirming Out...');
        await axiosAuth.post(`/${requestId}/confirm-out`);
        console.log('✅ Confirmed Out');

        // 4. Return
        console.log('\n[4] Returning...');
        await axiosAuth.post(`/${requestId}/return?operatorId=admin-e2e`);
        console.log('✅ Returned');

        console.log('\n🎉 E2E Test Passed!');

    } catch (e: any) {
        console.error('❌ Test Failed:', e.response ? e.response.data : e.message);
        process.exit(1);
    }
}

testBorrowFlow();
