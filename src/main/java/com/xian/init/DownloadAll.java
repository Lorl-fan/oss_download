package com.xian.init;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.xian.Utils.BaseInfo;
import com.xian.Utils.FileUtil;
import com.xian.Utils.OssUtil;
import com.xian.base.ReInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Slf4j
@Component
public class DownloadAll implements CommandLineRunner{

    @Autowired
    private BaseInfo baseInfo;
    @Autowired
    private OssUtil ou;

    /**
     * 初始download，endTime为Timer开始运行时间
     * downloadTask执行时间为每天凌晨1点
     * 从First.endTime开始检索检索结束时间为当天23:59:59
     * 第二次开始时间为上次执行检索结束时间，结束时间为当天23:59:59
     * 以此类推~~
     */
    @Override
    public void run(String... args) throws Exception {
        begin();
    }

    private void begin(){

        String localFileDir = baseInfo.localFileDir;
        String ossDownLoadLog = baseInfo.ossDownLoadLog;
        String endpoint = baseInfo.endpoint;
        String accessKeyId = baseInfo.accessKeyId;
        String accessKeySecret = baseInfo.accessKeySecret;
        String fileLocation = localFileDir+ossDownLoadLog;

        //判断文件是否存在，并且文件第一行的状态是否是1（全量完成状态：1、未完成：0）
        File file = new File(fileLocation);
        byte[] bytes = null;
        if(file.exists()){
            //当文件存在，判断第一行状态是否为1，为1跳过全量下载
            bytes = FileUtil.readFile(fileLocation);
            String str = new String(bytes);
            String[] strArr = str.split("\n");
            String[] strFirstRow = strArr[0].split("\\|");
            if(strFirstRow.length>1){
                String num = strFirstRow[1];
                int n = Integer.parseInt(num);
                if(n>0){
                    return;
                }
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.set(2019,0,1,0,0,0);
        Date startDate = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");
        //初始化log
        String initStr = "cronJob|0\nfirshStatus|0\n";
        String newDateStr = sdf.format(startDate);
        initStr = initStr + newDateStr + "|" + newDateStr + "\n";
        FileUtil.writeFile(fileLocation,initStr.getBytes());

        //设置endDate
        Date endDate = new Date();
        //建立连接
        log.info("~~~~~~ 建立连接 ~~~~~~~");
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        String bucketName = baseInfo.bucketName;
        //ReInfo reInfo = new ReInfo(1,2,true);
        ReInfo reInfo = ou.digui(ossClient, bucketName, null, startDate.getTime(), endDate.getTime(),null);
        log.info("~~~~~~ 关闭连接 ~~~~~~");
        ossClient.shutdown();
        if(reInfo.isFlag()){
            //全量完成更新log
            String initStr1 = "cronJob|1\nfirshStatus|1\n" + newDateStr + "|" + sdf.format(endDate) + "\n" + reInfo.getNewFolderCount() + "|" + reInfo.getNewFileCount() + "\n";
            FileUtil.writeFile(fileLocation,initStr1.getBytes());
        }

    }
}
