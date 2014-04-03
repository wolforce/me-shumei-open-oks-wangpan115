package me.shumei.open.oks.wangpan115;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.content.Context;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * @author wolforce
 *
 */
public class Signin extends CommonData {
    String resultFlag = "false";
    String resultStr = "未知错误！";
    
    /**
     * <p><b>程序的签到入口</b></p>
     * <p>在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功</p>
     * @param ctx 主程序执行签到的Service的Context，可以用此Context来发送广播
     * @param isAutoSign 当前程序是否处于定时自动签到状态<br />true代表处于定时自动签到，false代表手动打开软件签到<br />一般在定时自动签到状态时，遇到验证码需要自动跳过
     * @param cfg “配置”栏内输入的数据
     * @param user 用户名
     * @param pwd 解密后的明文密码
     * @return 长度为2的一维String数组<br />String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />String[1]表示返回的成功或出错信息
     */
    public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
        //把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
        CaptchaUtil.context = ctx;
        //标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
        CaptchaUtil.isAutoSign = isAutoSign;
        
        try{
            //存放Cookies的HashMap
            HashMap<String, String> cookies = new HashMap<String, String>();
            //Jsoup的Response
            Response res;
            
            String baseUrl = "http://115.com";//115主页
            String loginUrl = "http://passport.115.com//?ct=login&ac=ajax&is_ssl=1";//登录链接
            String signInfoUrl = "http://115.com/?ct=event&ac=get_active_param";//获取签到信息的URL
            String signSubmitUrl = "http://115.com/?ct=ajax_user&ac=checkin";//提交签到请求的URL
            String yaoPageUrl = "http://115.com/?ct=yao";//摇奖页面URL
            String yaoSignUrl = "http://115.com/?ct=ajax_user&ac=pick_spaces&u=1&token=";//摇奖的签到链接
            //String signUrl = "http://115.com/?ct=ajax_user&ac=pick_spaces&u=1&token=cecb75ae9c54e04285d968c0ef31234b&_=1366028854220";//签到链接
            
            //签到与摇奖所用的token，可以在获取的签到信息中得到
            String token = "";
            
            //访问115网盘首页
            res = Jsoup.connect(baseUrl).userAgent(UA_CHROME).referrer(baseUrl).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
            cookies.putAll(res.cookies());
            
            //提取SSO信息，进行两手准备，防止Jsoup获取出错
            String back = "http://www.115.com";
            String timeStr = String.valueOf(new Date().getTime());
            HashMap<String, String> postDatas = new HashMap<String, String>();
            postDatas.put("login[ssoln]", user);
            postDatas.put("login[ssopw]", SHA1.encode(SHA1.encode(SHA1.encode(pwd) + SHA1.encode(user)) + timeStr));//return sha1( sha1(sha1(pwd)+sha1(account) ) + vcode.toUpperCase());
            postDatas.put("login[ssovcode]", timeStr);
            postDatas.put("login[ssoent]", "A1");
            postDatas.put("login[version]", "2.0");
            postDatas.put("login[ssoext]", timeStr);
            postDatas.put("login[time]", "0");
            postDatas.put("back", back);
            
            //登录网站
            //{"state":false,"goto":"http:\/\/passport.115.com\/?ct=login&ac=gotos&goto=","err_msg":"你的密码不正确，请重新输入！","err_code":70005,"err_name":"passwd"}
            res = Jsoup.connect(loginUrl)
                    .data(postDatas)
                    .header("Host", "passport.115.com")
                    .header("Origin", "http://www.115.com")
                    .userAgent(UA_CHROME).timeout(TIME_OUT).referrer(baseUrl).ignoreContentType(true).method(Method.POST).execute();
            cookies.putAll(res.cookies());
            //登录失败
            if (res.body().contains("\"state\":false")) {
                JSONObject infoObj = new JSONObject(res.body());
                return new String[]{"false", infoObj.optString("err_msg")};
            }
            
            //开始签到
            //获取签到信息
            //{"state":true,"is_take":true,"is_take_token":"b3db7f01234dcf4ccd1234fc7cbc1234","is_checkin":"1","this_turn":0,"invite_url":"http:\/\/115.com\/invite\/6941bc7b"}
            //{"state":true,"is_take":true,"is_take_token":"b3db7f01234dcf4ccd1234fc7cbc1234","is_checkin":0,"this_turn":0,"invite_url":"http:\/\/115.com\/invite\/6941bc7b"}
            boolean signinResultFlag = false;
            String signinResultStr = "";
            res = Jsoup.connect(signInfoUrl).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).referrer(baseUrl).ignoreContentType(true).method(Method.GET).execute();
            cookies.putAll(res.cookies());
            //System.out.println(res.body());
            JSONObject jsonObj = new JSONObject(res.body());
            token = jsonObj.optString("is_take_token");
            if (jsonObj.optBoolean("state")) {
                int is_checkin = jsonObj.optInt("is_checkin");
                if (is_checkin == 1) {
                    signinResultStr = "今天已签到 第" + jsonObj.optInt("this_turn") + "天\n";
                    signinResultFlag = true;
                } else {
                    //提交签到请求
                    //{"state":true,"data":{"this_turn":1,"this_turn_space":"1GB","next_turn":2,"next_turn_space":"2GB"}}
                    //{"state":false,"err_code":200021,"err_msg":"今天已经签到了"}
                    res = Jsoup.connect(signSubmitUrl).cookies(cookies).data("token", token).userAgent(UA_CHROME).timeout(TIME_OUT).referrer(baseUrl).ignoreContentType(true).method(Method.POST).execute();
                    cookies.putAll(res.cookies());
                    jsonObj = new JSONObject(res.body());
                    if (jsonObj.optBoolean("state")) {
                        JSONObject dataObj = jsonObj.optJSONObject("data");
                        StringBuilder sb = new StringBuilder("签到成功，");
                        sb.append("第" + dataObj.optInt("this_turn") + "天");
                        sb.append("本次奖励" + dataObj.optString("this_turn_space") + "，");
                        sb.append("下次奖励" + dataObj.optString("next_turn_space") + "\n");
                        signinResultStr = sb.toString();
                        signinResultFlag = true;
                    } else {
                        signinResultStr = jsonObj.optString("err_msg") + "\n";
                    }
                    System.out.println(res.body());
                }
            } else {
                signinResultStr = "115服务器错误，无法连接签到服务器\n";
            }
            
            //开始摇奖
            //访问摇奖页面并用正则表达式提取包含在JS中的token
            res = Jsoup.connect(yaoPageUrl).cookies(cookies).userAgent(UA_CHROME).timeout(TIME_OUT).referrer(baseUrl).ignoreContentType(true).method(Method.GET).execute();
            cookies.putAll(res.cookies());
            
            //用正则查找token
            Pattern pattern = Pattern.compile("take_token.*=.*'(.+)'");
            Matcher matcher = pattern.matcher(res.parse().html());
            if(matcher.find())
            {
                //0=>take_token = 'b3db7f01234dcf4ccd1234fc7cbc1234'
                //1=>b3db7f01234dcf4ccd1234fc7cbc1234
                token = matcher.group(1);
                yaoSignUrl = yaoSignUrl + token + "&_=" + new Date().getTime();
                res = Jsoup.connect(yaoSignUrl).cookies(cookies).userAgent(UA_ANDROID).timeout(TIME_OUT).referrer(baseUrl).ignoreContentType(true).method(Method.GET).execute();
                
                //{"state":true,"picked":"115MB","picked_num":115,"flag":false,"total_size":"33135MB","used_percent":"1%","exp":791}
                String signReturnStr = res.body();
                jsonObj = new JSONObject(signReturnStr);
                Boolean state = jsonObj.getBoolean("state");
                if(state)
                {
                    //签到成功
                    resultFlag = "true";
                    //防止VIP账号获取的JSON不对
                    try {
                        String picked = jsonObj.getString("picked");
                        String totalSize = jsonObj.getString("total_size");
                        int exp = jsonObj.getInt("exp");
                        resultStr = "摇奖成功，获得"+ picked + "空间，账户总容量" + totalSize + "，总经验值" + exp;
                    } catch (Exception e) {
                        resultStr = "摇奖成功";
                    }
                }
                else
                {
                    resultFlag = "false";
                    if(jsonObj.has("msg"))
                        resultStr = jsonObj.getString("msg");
                    else
                        resultStr = "登录成功，但提交摇奖请求后返回失败信息";
                }
            }
            else if(res.parse().html().contains("输入115网盘帐号/手机/邮箱"))
            {
                resultFlag = "false";
                resultStr = "登录失败";
            }
            else
            {
                resultFlag = "false";
                resultStr = "已摇过奖到或第二次摇奖机会还没开启\n\n115网盘每日可摇奖两次，第一次机会一般是中午12点前，第二次是12点后";
            }
            
            //只有签到和摇奖都成功整个任务才算签到成功
            if (signinResultFlag && resultFlag.equals("true")) {
                resultFlag = "true";
                resultStr = signinResultStr + "\n" + resultStr;
            } else {
                resultFlag = "false";
                resultStr = signinResultStr + "\n" + resultStr;
            }
            
        } catch (IOException e) {
            this.resultFlag = "false";
            this.resultStr = "连接超时";
            e.printStackTrace();
        } catch (Exception e) {
            this.resultFlag = "false";
            this.resultStr = "未知错误！";
            e.printStackTrace();
        }
        
        return new String[]{resultFlag, resultStr};
    }
    
    
}
