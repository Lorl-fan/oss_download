package com.xian.Utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.xian.base.ReInfo;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class OssUtil {

    private long objTime;
    @Value("${localFileDir}")
    private String localFileDir;
    @Value("${bucketName}")
    private String bucketName;
    private static long newFileCount;
    private static long newFolderCount;
    private static int recursionCount;
    private ReInfo rInfo = new ReInfo();

    /**
     * 递归查询
     * @param ossClient oss连接
     * @param bucketName bucket名
     * @param marker 续查位置
     * @param dateStart 检索区间开始
     * @param dateEnd 检索区间结束
     */
    public ReInfo digui(OSS ossClient, String bucketName , String marker, long dateStart, long dateEnd, String prefix) {
        rInfo.setFlag(false);
        ObjectListing objectListing;
        //初始状态marker为空
        if(marker==null) {
            // test + .withPrefix("2019122519305")
            objectListing = ossClient
                    .listObjects(new ListObjectsRequest(bucketName).withMaxKeys(1000).withPrefix(prefix));
        }else {
        //未遍历完marker为上次结束
            objectListing = ossClient
                    .listObjects(new ListObjectsRequest(bucketName).withMaxKeys(1000).withMarker(marker).withPrefix(prefix));
        }

        List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
        for (OSSObjectSummary objectSummary : sums) {
            downloadFile(ossClient, objectSummary,marker,dateStart,dateEnd);
        }
        marker = objectListing.getNextMarker();
        log.info("********** " + marker + " ***********");
        // objectListing.isTruncated() == true 为被截断，则继续查询
        log.info("递归次数 " + recursionCount);
        recursionCount++;
        if (objectListing.isTruncated()) {
            log.info("未完成查询，继续递归");
            digui(ossClient, bucketName,marker,dateStart,dateEnd,prefix);
        }else{
            log.info("遍历下载完成");
            rInfo.setFlag(true);
            rInfo.setNewFileCount(newFileCount);
            rInfo.setNewFolderCount(newFolderCount);
        }
        return rInfo;
    }

    /**
     * 文件下载
     * @param ossClient oss连接
     * @param ossObjectSummary oss对象
     * @param marker 续查位置
     * @param dateStart 检索区间开始
     * @param dateEnd 检索区间结束
     */
    public void downloadFile(OSS ossClient, OSSObjectSummary ossObjectSummary, String marker, long dateStart,long dateEnd) {
        String key = ossObjectSummary.getKey();
        if (ossObjectSummary.getSize() > 0) {
            String location = localFileDir + key;
//			File ff = new File(location);
            // TODO 测试时添加 ， 如果没有才下载 正常应为有则更新
//			if (!ff.exists()) {
//			printLog("~~~~~~~~~~~  当前Key不存在于目标文件夹，新建下载任务！！  ~~~~~~~~~~~");
            marker = key;
            log.info("当前key为 ： " + ossObjectSummary.getKey());
            //当前oss Object 最后修改时间大于开始时间小于结束时间
            objTime = ossObjectSummary.getLastModified().getTime();
            if(objTime>dateStart&&objTime<dateEnd) {
                log.info("当前key符合下载时间要求 ！~~~~~~~~~~~~  开始下载任务！！");
                String[] keyArr = location.split("/");
                location = location.replace(keyArr[keyArr.length - 1], "");
                File f = new File(location);
                // 创建文件夹
                if (!f.exists()) {
                    f.mkdirs();
                    newFolderCount++;
                    log.info("新建文件夹数量" + newFolderCount);
                }
                // 下载请求，10个任务并发下载，启动断点续传。
                DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName,
                        ossObjectSummary.getKey());
                downloadFileRequest.setDownloadFile(localFileDir + key);
                downloadFileRequest.setPartSize(1 * 1024 * 1024);
                downloadFileRequest.setTaskNum(10);
                downloadFileRequest.setEnableCheckpoint(true);
                downloadFileRequest.setCheckpointFile(localFileDir + "1.txt");
//				downloadFileRequest.setModifiedSinceConstraint(yesterdayEnd);
//				downloadFileRequest.setUnmodifiedSinceConstraint(yesterdayStart);

                // 下载文件。
                DownloadFileResult downloadRes;
                try {
                    // 下载成功时，会返回文件元信息。
                    downloadRes = ossClient.downloadFile(downloadFileRequest);
                    newFileCount++;
                    log.info("~~~~~~  下载成功  ~~~~~~~ 当前文件元数据信息 ： " + downloadRes.getObjectMetadata());
                    log.info("文件数量：" + newFileCount);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }else {
                log.info("未符合下载时间条件~！~~~~~~~~~~~~~~");
            }
//			} else {
//				printLog("此文件已存在 : " + location);
//			}
        } else {
            log.info(" 判断当前为目录  " + key);
        }
    }

}
