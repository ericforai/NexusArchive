#!/bin/bash

# Configuration
ARCHIVE_ROOT="/tmp/archives"
FONDS="COMP001"
YEAR="2023"
RETENTION="10Y"
CATEGORY="AC01"

# Demo Archival Codes
CODES=(
  "COMP001-2023-10Y-FIN-AC01-V0051"
  "COMP001-2023-10Y-FIN-AC01-V0052"
  "COMP001-2023-10Y-FIN-AC01-V0053"
  "COMP001-2023-10Y-FIN-AC01-V0098"
  "COMP001-2023-10Y-FIN-AC01-V0095"
)

echo "Setting up demo AIP packages in $ARCHIVE_ROOT..."

for CODE in "${CODES[@]}"; do
  # Construct path
  DIR="$ARCHIVE_ROOT/$FONDS/$YEAR/$RETENTION/$CATEGORY/$CODE/content"
  
  # Create directory
  mkdir -p "$DIR"
  
  # Create dummy voucher.pdf
  if [ ! -f "$DIR/voucher.pdf" ]; then
    echo "%PDF-1.4 dummy content for $CODE" > "$DIR/voucher.pdf"
  fi
  
  # Create dummy invoice.jpg
  if [ ! -f "$DIR/invoice.jpg" ]; then
    echo "dummy image content" > "$DIR/invoice.jpg"
  fi
  
  # Create dummy metadata.xml
  if [ ! -f "$DIR/metadata.xml" ]; then
    cat > "$DIR/metadata.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<eep>
  <header>
    <version>1.0</version>
    <archivalCode>$CODE</archivalCode>
    <createdTime>$(date -Iseconds)</createdTime>
  </header>
  <entityData>
    <fondsCode>$FONDS</fondsCode>
    <accountPeriod>$YEAR-01</accountPeriod>
    <voucherNumber>${CODE##*-}</voucherNumber>
  </entityData>
</eep>
EOF
  fi
  
  echo "Created AIP for $CODE"
done

# Special Case for Structured AIP Demo (V0088)
V88_CODE="COMP001-2023-10Y-FIN-AC01-V0088"
V88_DIR="$ARCHIVE_ROOT/$FONDS/$YEAR/$RETENTION/$CATEGORY/$V88_CODE/content"
mkdir -p "$V88_DIR"

# 1. Main Voucher (OFD) -> Create a valid ZIP structure (OFD container)
if [ ! -f "$V88_DIR/voucher_v0088.ofd" ]; then
  echo "<?xml version='1.0' encoding='UTF-8'?><OFD><DocBody><DocInfo><DocID>V0088</DocID></DocInfo></DocBody></OFD>" > "$V88_DIR/OFD.xml"
  # Use jar to create zip (OFD is a zip format)
  if command -v jar &> /dev/null; then
    jar -cMf "$V88_DIR/voucher_v0088.ofd" -C "$V88_DIR" OFD.xml
  else
    # Fallback to zip if jar not found (though java is req)
    cd "$V88_DIR" && zip -q voucher_v0088.ofd OFD.xml && cd - > /dev/null
  fi
  rm "$V88_DIR/OFD.xml"
fi

# 2. Attachment (Contract) -> Copy real PDF from frontend assets
# Find the invoice pdf in frontend dir
REAL_PDF=$(find frontend -name "dzfp_*.pdf" | head -n 1)
if [ -f "$REAL_PDF" ]; then
  cp "$REAL_PDF" "$V88_DIR/contract_2023_001.pdf"
else
  # Fallback: Minimal Valid PDF
  echo "%PDF-1.0
1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj 2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj 3 0 obj<</Type/Page/MediaBox[0 0 3 3]/Parent 2 0 R/Resources<<>>>>endobj
xref
0 4
0000000000 65535 f
0000000010 00000 n
0000000060 00000 n
0000000157 00000 n
trailer<</Size 4/Root 1 0 R>>
startxref
249
%%EOF" > "$V88_DIR/contract_2023_001.pdf"
fi

# 3. Attachment (Bank Slip) -> Valid JPG (Base64)
if [ ! -f "$V88_DIR/bank_slip_001.jpg" ]; then
  # 1x1 White Pixel JPG
  echo "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwH3+iiigD//" | base64 -d > "$V88_DIR/bank_slip_001.jpg"
fi
echo "Created Structured AIP Demo for $V88_CODE"

# Also ensure permissions are open (since we run as root but maybe app drops privs? unlikely based on service file)
chmod -R 777 "$ARCHIVE_ROOT"

echo "Demo AIP setup complete."
