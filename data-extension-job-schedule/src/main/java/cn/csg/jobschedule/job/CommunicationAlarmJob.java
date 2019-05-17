package cn.csg.jobschedule.job;

import cn.csg.jobschedule.constants.DatetimeConstants;
import cn.csg.jobschedule.service.MetadataService;
import cn.csg.jobschedule.util.DatetimeUtil;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by hjw on 2019-04-22
 * 爆发式通信对告警统计
 */
@Component
@EnableScheduling
public class CommunicationAlarmJob {

    private final static Logger logger = LoggerFactory.getLogger(CommunicationAlarmJob.class);

    @Autowired
    public MetadataService metadataService;


    /**
     * 1.srcIp在t分钟内发起n次访问
     */
    public void executeSrcIpSumTask() {
        try {
            List dataList = metadataService.getAlarmRule("srcIpSum");
            if (dataList != null && dataList.size() > 0) {
                Map<String, Long> ruleValueMap = getRuleValue(dataList);
                Map<String, Date> ranTimeMap = getRangTime(ruleValueMap.get("rangValue"));
                String srcIpSumQueryStr = getSrcIpSumQueryStr(ruleValueMap.get("thresholdValue"), ranTimeMap.get("startDelayTime"), ranTimeMap.get("endDelayDate"));
                Long cycle = (ruleValueMap.get("rangValue")/1000/60);
                logger.info("#############srcIp在"+(ruleValueMap.get("rangValue")/1000/60)+"分钟内发起"+ruleValueMap.get("thresholdValue")+"次访问 job正在执行########");
                JSONObject srcIpSumJson = metadataService.getResultByHttp(srcIpSumQueryStr);
                if(srcIpSumJson != null && srcIpSumJson.size()>0){
                    metadataService.handleSrcIpSumData(ruleValueMap.get("thresholdValue"),cycle,srcIpSumJson);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("srcIp在t分钟内发起n次访问执行异常：" + e.getMessage());
        }
    }

    /**
     * 2.srcIp在t分钟内访问了n个destIp
     */
    public void executeSrcIpAndDestIpCountTask() {
        try {
            List dataList = metadataService.getAlarmRule("srcIpAndDestIpCount");
            if (dataList != null && dataList.size() > 0) {
                Map<String, Long> ruleValueMap = getRuleValue(dataList);
                Map<String, Date> ranTimeMap = getRangTime(ruleValueMap.get("rangValue"));
                Long cycle = (ruleValueMap.get("rangValue")/1000/60);
                logger.info("&&&&&&&&&&srcIp在"+(ruleValueMap.get("rangValue")/1000/60)+"分钟内访问了"+ruleValueMap.get("thresholdValue")+"个destIp 正在执行&&&&&&&&");
                String srcIpAndDestIpCountQueryStr = getSrcIpAndDestIpCountQueryStr(ruleValueMap.get("thresholdValue"), ranTimeMap.get("startDelayTime"), ranTimeMap.get("endDelayDate"));
                JSONObject srcIpAndDestIpCountJson = metadataService.getResultByHttp(srcIpAndDestIpCountQueryStr);
                if(srcIpAndDestIpCountJson != null && srcIpAndDestIpCountJson.size()>0){
                    metadataService.handleSrcIpAndDestIpCountData(ruleValueMap.get("thresholdValue"),cycle,srcIpAndDestIpCountJson);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("srcIp在t分钟内发起n次访问执行异常：" + e.getMessage());
        }
    }

    /**
     * 3.srcIp在t分钟内访问同一destIp的n个端口
     */
    public void executeSrcIpAndDestPortCountTask() {
        try {
            List dataList = metadataService.getAlarmRule("srcIpAndDestPortCount");
            if (dataList != null && dataList.size() > 0) {
                Map<String, Long> ruleValueMap = getRuleValue(dataList);
                Map<String, Date> ranTimeMap = getRangTime(ruleValueMap.get("rangValue"));
                Long cycle = (ruleValueMap.get("rangValue")/1000/60);
                logger.info("********srcIp在"+(ruleValueMap.get("rangValue")/1000/60)+"分钟内访问destIp的"+ruleValueMap.get("thresholdValue")+"个端口 正在执行********");
                String srcIpAndDestPortCountQueryStr = getSrcIpAndDestPortCountQueryStr(ranTimeMap.get("startDelayTime"), ranTimeMap.get("endDelayDate"));
                JSONObject srcIpAndDestPortCountJson = metadataService.getResultByHttp(srcIpAndDestPortCountQueryStr);
                if(srcIpAndDestPortCountJson != null && srcIpAndDestPortCountJson.size()>0){
                    metadataService.handleSrcIpAndDestPortCountData(ruleValueMap.get("thresholdValue"),cycle, srcIpAndDestPortCountJson);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("srcIp在t分钟内发起n次访问执行异常：" + e.getMessage());
        }
    }


    /**
     * (1)组装 srcIp在t分钟内发起n次访问 统计条件
     *
     * @param thresholdValue 统计阈值
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @return
     */
    private String getSrcIpSumQueryStr(Long thresholdValue, Date startTime, Date endTime) throws Exception {
        String queryStr = "{\n" +
                "    \"aggs\": {\n" +
                "        \"deviceGUIDCount\": {\n" +
                "            \"aggs\": {\n" +
                "                \"srcIpCount\": {\n" +
                "                    \"aggs\": {\n" +
                "                        \"doubleCountSum\": {\n" +
                "                            \"sum\": {\n" +
                "                                \"field\": \"count\"\n" +
                "                            }\n" +
                "                        },\n" +
                "                        \"doubleCount_filter\": {\n" +
                "                            \"bucket_selector\": {\n" +
                "                                \"buckets_path\": {\n" +
                "                                    \"filterKey\": \"doubleCountSum\"\n" +
                "                                },\n" +
                "                                \"script\": \"params.filterKey >= " + thresholdValue + "\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    },\n" +
                "                    \"terms\": {\n" +
                "                        \"field\": \"srcIp.keyword\"\n" +
                "                    }\n" +
                "                }\n" +
                "            },\n" +
                "            \"terms\": {\n" +
                "                \"field\": \"dcdGuid.keyword\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"query\": {\n" +
                "        \"range\": {\n" +
                "            \"sessionStartTime\": {\n" +
                "                \"gte\": \"" + DatetimeUtil.toStr(startTime, DatetimeConstants.YYYY_MM_DD_T_HH_MM_SS_XXX) + "\",\n" +
                "                \"lt\": \"" + DatetimeUtil.toStr(endTime, DatetimeConstants.YYYY_MM_DD_T_HH_MM_SS_XXX) + "\"\n" +
//                "                \"gte\": \"2018-09-27T08:00:00+08:00\",\n" +
//                "                \"lt\": \"2018-09-27T20:00:00+08:00\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"size\": 0\n" +
                "}";
        return queryStr;
    }

    /**
     * (2)组装 srcIp在t分钟内访问了n个destIp 统计条件
     *
     * @param thresholdValue 阈值
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @return
     */
    private String getSrcIpAndDestIpCountQueryStr(Long thresholdValue, Date startTime, Date endTime) throws Exception {
        String queryStr = "{\n" +
                "    \"aggs\": {\n" +
                "        \"deviceGUIDCount\": {\n" +
                "            \"aggs\": {\n" +
                "                \"srcIpCount\": {\n" +
                "                    \"aggs\": {\n" +
                "                        \"destIpCount\": {\n" +
                "                            \"terms\": {\n" +
                "                                \"field\": \"destIp.keyword\"\n" +
                "                            }\n" +
                "                        },\n" +
                "                        \"having\": {\n" +
                "                            \"bucket_selector\": {\n" +
                "                                \"buckets_path\": {\n" +
                "                                    \"srcIpCount\": \"_count\"\n" +
                "                                },\n" +
                "                                \"script\": {\n" +
                "                                    \"source\": \"params.srcIpCount >= " + thresholdValue + "\"\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                    },\n" +
                "                    \"terms\": {\n" +
                "                        \"field\": \"srcIp.keyword\"\n" +
                "                    }\n" +
                "                }\n" +
                "            },\n" +
                "            \"terms\": {\n" +
                "                \"field\": \"dcdGuid.keyword\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"query\": {\n" +
                "        \"range\": {\n" +
                "            \"sessionStartTime\": {\n" +
                "                \"gte\": \"" + DatetimeUtil.toStr(startTime, DatetimeConstants.YYYY_MM_DD_T_HH_MM_SS_XXX) + "\",\n" +
                "                \"lt\": \"" + DatetimeUtil.toStr(endTime, DatetimeConstants.YYYY_MM_DD_T_HH_MM_SS_XXX) + "\"\n" +
//                "                \"gte\": \"2018-09-27T08:00:00+08:00\",\n" +
//                "                \"lt\": \"2018-09-27T20:00:00+08:00\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"size\": 0\n" +
                "}";
        return queryStr;
    }

    /**
     * (3)组装 srcIp在t分钟内访问同一destIp的n个端口 查询条件
     *
     * @param startTime      开始时间
     * @param endTime        结束时间
     * @return
     */
    private String getSrcIpAndDestPortCountQueryStr(Date startTime, Date endTime) throws Exception {
        String queryStr = "{\n" +
                "    \"aggs\": {\n" +
                "        \"deviceGUIDCount\": {\n" +
                "            \"aggs\": {\n" +
                "                \"srcIpCount\": {\n" +
                "                    \"aggs\": {\n" +
                "                        \"destIpCount\": {\n" +
                "\t\t\t\t\t\t  \"aggs\": {\n" +
                "                                \"destPortCount\": {\n" +
                "                                    \"terms\": {\n" +
                "                                        \"field\": \"destPort.keyword\"\n" +
                "                                    }\n" +
                "                                }\n" +
                "                            },\n" +
                "                            \"terms\": {\n" +
                "                                \"field\": \"destIp.keyword\"\n" +
                "                            }\n" +
                "                        }\n" +
                "                    },\n" +
                "                    \"terms\": {\n" +
                "                        \"field\": \"srcIp.keyword\"\n" +
                "                    }\n" +
                "                }\n" +
                "            },\n" +
                "            \"terms\": {\n" +
                "                \"field\": \"dcdGuid.keyword\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"query\": {\n" +
                "        \"range\": {\n" +
                "            \"sessionStartTime\": {\n" +
                "                \"gte\": \"" + DatetimeUtil.toStr(startTime, DatetimeConstants.YYYY_MM_DD_T_HH_MM_SS_XXX) + "\",\n" +
                "                \"lt\": \"" + DatetimeUtil.toStr(endTime, DatetimeConstants.YYYY_MM_DD_T_HH_MM_SS_XXX) + "\"\n" +
//                "                \"gte\": \"2018-09-27T08:00:00+08:00\",\n" +
//                "                \"lt\": \"2018-09-27T20:00:00+08:00\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"size\": 0\n" +
                "}";

        return queryStr;
    }

    /**
     * 将时间范围转换为开始、结束时间
     *
     * @param rangTime 查询时间范围，单位：秒
     * @return
     */
    private static Map<String, Date> getRangTime(Long rangTime) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long time = rangTime * 1000;
        long endTime = System.currentTimeMillis();
        long startTime = endTime - time;
        Date startDate = new Date();
        startDate.setTime(startTime);
        String startTimeStr = sdf.format(startDate) + ":00";
        String endTimeStr = sdf.format(endTime) + ":00";
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long startDelayTime = sd.parse(startTimeStr).getTime();
        long endDelayTime = sd.parse(endTimeStr).getTime();
        //延迟时间
        long delayTime = 1 * 1000 * 60;
        startDelayTime = startDelayTime - delayTime;
        endDelayTime = endDelayTime - delayTime;
        Date startDelayDate = new Date();
        startDelayDate.setTime(startDelayTime);
        Date endDelayDate = new Date();
        endDelayDate.setTime(endDelayTime);
        Map<String, Date> ranTimeMap = new HashMap<String, Date>();
        ranTimeMap.put("startDelayTime", startDelayDate);
        ranTimeMap.put("endDelayDate", endDelayDate);
        return ranTimeMap;
    }

    /**
     * 获取规则值
     *
     * @param dataList
     * @return
     */
    private Map<String, Long> getRuleValue(List dataList) {
        Map<String, Long> ruleValueMap = new HashMap<String, Long>();
        Iterator it = dataList.iterator();
        Long rangValue = 0L;
        Long thresholdValue = 0L;
        while (it.hasNext()) {
            Map dataMap = (Map) it.next();
            if ("window".equals(dataMap.get("rule_key") + "")) {
                rangValue = Double.valueOf(dataMap.get("rule_value") + "").longValue() * 1000L;
            }
            if ("target".equals(dataMap.get("rule_key") + "")) {
                thresholdValue = Double.valueOf(dataMap.get("rule_value") + "").longValue();
            }
        }
        ruleValueMap.put("rangValue", rangValue);
        ruleValueMap.put("thresholdValue", thresholdValue);
        return ruleValueMap;
    }
}
