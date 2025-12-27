
import psycopg2
import os

def check_invoice():
    try:
        try:
             conn = psycopg2.connect(dbname="nexusarchive", user="postgres", password="postgres", host="localhost", port="5432")
             print("Connected to nexusarchive:5432")
        except:
             try:
                 conn = psycopg2.connect(dbname="nexusarchive", user="postgres", password="postgres", host="localhost", port="54321")
                 print("Connected to nexusarchive:54321")
             except:
                 conn = psycopg2.connect(dbname="postgres", user="postgres", password="postgres", host="localhost", port="54321")
                 print("Connected to postgres:54321")
        cur = conn.cursor()
        # Delete Flyway 100 to allow re-run
        print("\nDeleting Flyway V100 record...")
        try:
            cur.execute("DELETE FROM flyway_schema_history WHERE version = '100'")
            conn.commit()
            print("Deleted V100.")
        except Exception as e:
            print(f"Failed to delete V100: {e}")
            conn.rollback()

        # Inspect Candidates (Updated for arc-2024-002)
        print("\nInspecting arc-2024-002:")
        cur.execute("SELECT id, title, amount FROM acc_archive WHERE id = 'arc-2024-002'")
        v = cur.fetchone()
        # Verify V100 Application
        if v and str(v[2]) == '657.00':
             print("VERIFICATION_SUCCESS: Invoice updated to 657.00")
        else:
             print(f"VERIFICATION_FAILED: Amount is {v[2]}")
            
        cur.close()
        conn.close()
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    check_invoice()
