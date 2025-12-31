// Input: 金仓适配器配置
// Output: 金仓 DDL 生成适配器
// Pos: NexusCore DB Adapter Kingbase
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter.kingbase;

import com.nexusarchive.core.adapter.BaseDbAdapter;

public final class KingbaseAdapter extends BaseDbAdapter {
    public KingbaseAdapter() {
        super("Kingbase", new KingbaseTypeMapping());
    }
}
