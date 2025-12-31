// Input: 达梦适配器配置
// Output: 达梦 DDL 生成适配器
// Pos: NexusCore DB Adapter Dameng
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter.dameng;

import com.nexusarchive.core.adapter.BaseDbAdapter;

public final class DamengAdapter extends BaseDbAdapter {
    public DamengAdapter() {
        super("Dameng", new DamengTypeMapping());
    }
}
