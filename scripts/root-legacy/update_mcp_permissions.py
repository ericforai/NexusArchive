import json

config_path = "/Users/user/.claude/settings.json"

# Read current config
with open(config_path, "r") as f:
    config = json.load(f)

# MCP permissions to add
mcp_permissions = [
    "mcp__*",
    "ListMcpResourcesTool",
    "ReadMcpResourceTool"
]

# Get existing permissions and remove old MCP entries for clean update
existing = config.get("permissions", {}).get("allow", [])
new_permissions = [x for x in existing if not x.startswith("mcp__") and x not in ["ListMcpResourcesTool", "ReadMcpResourceTool"]]

# Merge with new MCP permissions
new_permissions.extend(mcp_permissions)

# Update config
config["permissions"]["allow"] = new_permissions

# Write back
with open(config_path, "w") as f:
    json.dump(config, f, indent=2)

print("MCP permissions updated successfully")
print(f"Added {len(mcp_permissions)} permissions")
