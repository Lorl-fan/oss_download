package com.xian.Task;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.xian.Utils.BaseInfo;
import com.xian.Utils.FileUtil;
import com.xian.Utils.OssUtil;
import com.xian.base.ReInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Slf4j
@Configuration
@EnableScheduling
public class CronJob {

    @Autowired
    private BaseInfo baseInfo;
    @Autowired
    private OssUtil ou;

    //@Scheduled(cron = "0 53 15 * * ? ")
    @Scheduled(cron = "0 0 1 * * ?")
    public void downloadFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd");

        String endpoint = baseInfo.endpoint;
        String accessKeyId = baseInfo.accessKeyId;
        String accessKeySecret = baseInfo.accessKeySecret;
        String localFileDir = baseInfo.localFileDir;
        String ossDownLoadLog = baseInfo.ossDownLoadLog;
        String bucketName = baseInfo.bucketName;
        //TODO 先判断初始下载是否完成，判断条件为文件中（初始时间，是否完成）两字段
        /**
         * 先判断初始下载是否完成，已完成继续下载增量，未完成等待下一次运行
         * 初始下载完成
         *     以初始下载时间为开始时间
         *     以运行当前时间（每天凌晨一点）之前一天的 23:59:59 为结束时间
         *     执行下载任务
         */

        String fileLocation = localFileDir + ossDownLoadLog;

        //判断文件是否存在，并且文件第一行的状态是否是1（全量完成状态：1、未完成：0）
        File file = new File(fileLocation);
        byte[] bytes = null;
        //当文件存在，判断第一行状态是否为1，为1跳过全量下载
        bytes = FileUtil.readFile(fileLocation);
        String str = new String(bytes);
        String[] strArr = str.split("\n");
        if (strArr.length > 3) {
            String[] strFirstRow = strArr[0].split("\\|");
            String[] strSecondRow = strArr[1].split("\\|");
            String[] strThirdRow = strArr[2].split("\\|");
            String[] strFourthRow = strArr[3].split("\\|");
            //cronJob开启，firstStatus=1，时间不一样（正确记录完成时间），两个数量值（正确记录新增文件夹数和新增文件数）
            //判断全量已完成
            if ("1".equals(strFirstRow[1])&&"1".equals(strSecondRow[1])&&strThirdRow[0]!=strThirdRow[1]&&strFourthRow.length==2) {
                //由于程序设定，第一行状态为任务是否正在执行（0：未开始任务，1：已开始未执行，2：已开始正在执行）
                //设置增量开始标志
                strArr[0] = "cronJob|2";
                StringBuffer stringBuffer = new StringBuffer();
                for (String s : strArr) {
                    stringBuffer.append(s).append("\n");
                }
                log.info(stringBuffer.toString());
                FileUtil.writeFile(fileLocation,(stringBuffer.toString()).getBytes());
                //所以查询倒数第二行设定时间是否相同可以判断当前全量或增量是否完成
                String[] dateStrArr = strArr[strArr.length - 2].split("\\|");
                String dateStr = dateStrArr[1];
                Date startDate = null;
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE,-1);
                Date endDate = calendar.getTime();
                String endDateStr = sdf2.format(endDate);
                endDateStr += " 23:59:59";
                try {
                    startDate = sdf.parse(dateStr);
                    endDate = sdf.parse(endDateStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                log.info("~~~~~~ 建立连接 ~~~~~~");
                ReInfo reInfo = ou.digui(ossClient, bucketName, null, startDate.getTime(), endDate.getTime(),null);
                log.info("~~~~~~ 关闭连接 ~~~~~~");
                ossClient.shutdown();
                if(reInfo.isFlag()){
                    byte[] bytes1 = FileUtil.readFile(fileLocation);
                    String str1 = new String(bytes1);
                    String[] strArr2 = str1.split("\n");
                    if("2".equals(strArr2[0].split("\\|")[1])){
                        strArr2[0] = "cronJob|1";
                    }
                    StringBuffer stringBuffer1 = new StringBuffer();
                    for (String ss : strArr2){
                        stringBuffer1.append(ss).append("\n");
                    }
                    str1 = stringBuffer1.toString();
                    String newStr = sdf.format(startDate) + "|" + sdf.format(endDate) + "\n" + reInfo.getNewFolderCount() + "|" + reInfo.getNewFileCount() + "\n";
                    FileUtil.writeFile(fileLocation,(str1+newStr).getBytes());
                }else{
                    log.error("咋回事儿啊~~~");
                }
            } else {
                //cronJob 结束增量线程
                log.info("增量线程结束");
                log.error(strFirstRow[0] + "|" + strFirstRow[1]);
                return;
            }
        } else {
            log.error("strFirstRow.length <= 1");
        }
    }
}
