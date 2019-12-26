package com.xian.base;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReInfo {

    private long newFileCount;
    private long newFolderCount;
    private boolean flag;

    public ReInfo() {
    }

    public ReInfo(long newFileCount, long newFolderCount, boolean flag) {
        this.newFileCount = newFileCount;
        this.newFolderCount = newFolderCount;
        this.flag = flag;
    }
}
