package me.shumei.open.oks.wangpan115;

import android.content.Context;
import android.content.Intent;

/**
 * 存放一些公共的配置信息，如浏览器UA、网络超时时间、任务的重试次数等
 * @author wolforce
 *
 */
public class CommonData {
	
	//以下是一些公用信息，一般无需修改
	final static String UA_CHROME = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.152 Safari/535.19 CoolNovo/2.0.3.55";
	final static String UA_IE8 = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.0)";
	final static String UA_BAIDU_PC = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; baidubrowser 1.x)";
	final static String UA_BAIDU_ANDROID = "Mozilla/5.0 (Linux; U; Android 2.3.5; zh-cn; MI-ONE Plus Build/GINGERBREAD) AppleWebKit/533.1 (KHTML, like Gecko) FlyFlow/2.4 Version/4.0 Mobile Safari/533.1 baidubrowser/042_1.8.4.2_diordna_458_084/imoaiX_01_5.3.2_sulP-ENO-IM/100028m";
	final static String UA_ANDROID = "Mozilla/5.0 (Linux; U; Android 2.3.5; zh-cn; MI-ONE Plus Build/GINGERBREAD) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
	final static String UA_IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 5_0_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko)";
	final static int TIME_OUT = 30000;//超时时间为30秒
	final static int RETRY_TIMES = 3;//重试次数
	
	
	
	/**此常量的名称与数值都不可修改*/
	public static final String SRV_ACTION = "me.shumei.oks.signsrvaction";
	/**此常量的名称与数值都不可修改*/
	public static final int CMD_THREAD_SHOW_TOAST = 9999;
	
	/**
	 * 给《一键签到》主程序发送广播，让其在签到过程中用Toast显示信息
	 * @param context Context
	 * @param msg 要弹出的消息
	 * @param timetype true表示短时间的Toast，false表示长时间的Toast
	 */
	public static void sendShowToastBC(Context context, String msg, boolean timetype)
	{
		Intent intent = new Intent("me.shumei.oks.signsrvaction");
		intent.putExtra("cmd", CMD_THREAD_SHOW_TOAST);
		intent.putExtra("msg", msg);
		intent.putExtra("timetype", timetype);
		context.sendBroadcast(intent);
	}
	
	public CommonData() {
		// TODO Auto-generated constructor stub
	}

}
