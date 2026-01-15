import os
import subprocess
import uuid
import datetime
from pathlib import Path

# Paths
DATA_DIR = "/Users/user/nexusarchive/nexusarchive-java/data/archives/pre-archive"

def run_sql(sql):
    cmd = [
        "docker", "exec", "-i", "nexus-db", 
        "psql", "-U", "postgres", "-d", "nexusarchive"
    ]
    process = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    stdout, stderr = process.communicate(input=sql)
    if process.returncode != 0:
        print(f"SQL Error: {stderr}")
    return stdout

def import_files():
    path = Path(DATA_DIR)
    if not path.exists():
        print(f"Directory {DATA_DIR} does not exist.")
        return

    files = list(path.rglob("*.pdf"))
    print(f"Found {len(files)} PDF files in {DATA_DIR}")

    count = 0
    for file_path in files:
        file_name = file_path.name
        parts = file_name.split("_")
        biz_no = parts[0] if parts else file_name
        
        file_id = str(uuid.uuid4())
        archival_code = f"TEMP-IMPORT-{biz_no}"
        file_size = file_path.stat().st_size
        storage_path = str(file_path)
        now = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        today = datetime.date.today().strftime('%Y-%m-%d')
        
        # 1. Insert into arc_file_content
        sql_content = f"""
        INSERT INTO arc_file_content (
            id, archival_code, file_name, file_type, file_size, 
            file_hash, hash_algorithm, storage_path, pre_archive_status, 
            source_system, business_doc_no, erp_voucher_no, created_time
        ) VALUES (
            '{file_id}', '{archival_code}', '{file_name}', 'PDF', {file_size},
            'IMPORTED_{biz_no}', 'SM3', '{storage_path}', 'PENDING_CHECK',
            'YonSuite', '{biz_no}', '{biz_no}', '{now}'
        );
        """
        
        # 2. Insert into arc_file_metadata_index
        sql_meta = f"""
        INSERT INTO arc_file_metadata_index (
            file_id, invoice_number, total_amount, issue_date, 
            seller_name, parsed_time, parser_type
        ) VALUES (
            '{file_id}', '{biz_no}', 0.0, '{today}', 
            'Imported Entity', '{now}', 'MANUAL_IMPORT'
        );
        """
        
        full_sql = f"BEGIN; {sql_content} {sql_meta} COMMIT;"
        run_sql(full_sql)
        
        count += 1
        print(f"Imported: {file_name}")

    print(f"Total files imported: {count}")

if __name__ == "__main__":
    import_files()
