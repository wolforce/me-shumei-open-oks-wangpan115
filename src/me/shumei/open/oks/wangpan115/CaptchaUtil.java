package me.shumei.open.oks.wangpan115;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.content.Context;
import android.content.Intent;

/**
 * <p>本类类名为CaptchaUtil，不可改为其他名字</p>
 * 验证码操作类，《一键签到》主程序与插件程序之间操作验证码显示与输入的重要类，一般不需要作改动<br />
 * 
 * <p><b>使用方法：</b></p>
 * <blockquote><pre>
 * CaptchaUtil.context = ctx;//必须先把主程序的Context传递进来才能调用showCaptcha函数
 * if(CaptchaUtil.showCaptcha(captchaUrl, UA_CHROME, cookies, "百度贴吧", "shumei@shumei.me", "通行证需要验证码")) {
 * 	if(CaptchaUtil.captcha_input.length() > 0) {
 * 		//获取验证码成功，可以用CaptchaUtil.captcha_input继续做其他事了
 * 	} else {
 * 		//用户取消输入验证码
 * 	}
 * } else {
 * 	//拉取验证码失败，签到失败
 * }
 * </pre></blockquote>
 * 
 * <p><b>工作原理：</b></p>
 * 主程序传入一个Context，在showCaptcha函数执行时，会通过该Context把验证码图片下载到主程序在手机内部的Cache目录，
 * 然后发送一个广播给主程序，让主程序读取该验证码图片并弹出输入窗。
 * 在发送广播的同时，本插件调用pauseThread()函数，通过线程锁暂停主程序的签到线程。
 * 用户在主程序输入或取消验证码后，主程序把用户输入的验证码赋给本插件的CaptchaUtil.captcha_input，并对签到线程解锁。此时一个验证码输入过程完成。
 * 
 * @author wolforce
 *
 */
public class CaptchaUtil extends CommonData {
	/**此常量的名称与数值都不可修改*/
	public static final String SRV_ACTION = "me.shumei.oks.signsrvaction";//启动Service里的广播监听器的字符串，用发通知主程序弹出验证码窗口
	/**此常量的名称与数值都不可修改*/
	public static final int CMD_THREAD_SHOW_CAPTCHA = 20;//在Thread里发出的命令，显示验证码填写对话框
	/**此常量的名称与数值都不可修改*/
	public static final String REFRESH_CAPTCHA = "me.shumei.oks.refreshcaptcha";
	
	/**
	 * <p><b>此变量名规定为captcha_input，不可改为其他名字</b></p>
	 * 用户在验证码输入框输入验证码后，《一键签到》主程序会把验证码直接写入此变量<br />
	 * 写入完毕后会释放线程锁，此时即可使用用户输入的验证码继续进行其他操作
	 */
	public static String captcha_input = "";
	
	/**《一键签到》主程序的Context*/
	public static Context context;
	
	/**当前程序是否处于定时自动签到状态，如果是，则应该自动跳过验证码。默认为不是自动签到状态*/
	public static boolean isAutoSign = false;

	public CaptchaUtil() {
		// TODO Auto-generated constructor stub
	}
	
	
	/**
	 * <p>下载、显示、刷新验证码（包含有下载和显示操作）</p>
	 * 获取验证码失败会返回false<br />
	 * 用户正确输入验证码、用户取消输入验证码都会返回true，根据验证码的length可判断是正确输入还是取消输入，取消输入时，length为0
	 * @param captchaUrl 验证码URL
	 * @param ua User-Agent
	 * @param cookies
	 * @param siteName 站点名称
	 * @param user 任务的账号名
	 * @param reason 显示验证码的原因
	 * @return 是否成功完成了验证码输入操作<br />含验证码拉取、显示、用户输入这些过程，全部正确完成了才返回true，否则就是false
	 */
	public static boolean showCaptcha(String captchaUrl, String ua, HashMap<String, String> cookies, String siteName, String user, String reason)
	{
		boolean isSucceed = false;
		
		//不是自动签到的时候才拉取验证码
		if(isAutoSign == false) {
			//刷新验证码放在一个无限循环中
			while(true) {
				//先清空一遍验证码
				captcha_input = "";
				//调用拉取验证码并显示的函数
				isSucceed = downloadCaptchaPic(captchaUrl, ua, cookies, siteName, user, reason);
				//如果验证码没有标记为重试，那即使是没有获取到验证码，也要跳出刷新循环
				if(captcha_input.equals(REFRESH_CAPTCHA) == false) {
					break;
				}
			}
		}
		return isSucceed;
	}
	



	/**
	 * 下载验证码图片并保存到手机内部空间<br />
	 * 下载完后会发送一条广播给《一键签到》主程序，让其显示验证码输入框
	 * @param captchaUrl
	 * @param ua
	 * @param cookies
	 * @param siteName
	 * @param user
	 * @param reason
	 * @return
	 */
	private static boolean downloadCaptchaPic(String captchaUrl, String ua, HashMap<String, String> cookies, String siteName, String user, String reason)
	{
		Response res;
		boolean isSucceed = false;
		
		for(int i=0;i<RETRY_TIMES;i++)
		{
			try {
				res = Jsoup.connect(captchaUrl).cookies(cookies).userAgent(ua).timeout(TIME_OUT).ignoreContentType(true).referrer(captchaUrl).method(Method.GET).execute();
				cookies.putAll(res.cookies());
				try {
					deleteCaptchaFile();//删除遗留的验证码
					saveCaptchaToFile(res.bodyAsBytes());//保存验证码到文件
					sendShowCaptchaDialogBC(siteName, user, reason);//给《一键签到》主程序发送广播，让其显示验证码
					isSucceed = true;
					pauseThread();//用线程锁暂停签到线程，如果按下了验证码输入窗口的“确定”或“取消”，程序会对签到线程进行解锁
					break;//跳出重试
				} catch (Exception e) {
					//保存验证码到文件失败
					isSucceed = false;
					e.printStackTrace();
				}
			} catch (IOException e) {
				//拉取验证码失败
				isSucceed = false;
				e.printStackTrace();
			}
		}
		return isSucceed;
	}


	/**
	 * 删除遗留的验证码，以防影响下次显示
	 */
	private static void deleteCaptchaFile()
	{
		try {
			java.io.File captchaFile = new java.io.File(context.getCacheDir(), "me_shumei_oks_captcha.jpg");
			if(captchaFile.exists()) {
				captchaFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 保存验证码图片到手机内部的程序缓存目录（防止部分手机没有SD卡）
	 * @param b
	 * @throws IOException 
	 */
	private static void saveCaptchaToFile(byte[] b) throws IOException
	{
		FileOutputStream out = (new FileOutputStream(new java.io.File(context.getCacheDir(), "me_shumei_oks_captcha.jpg")));
		out.write(b);
		out.close();
	}
	
	
	/**
	 * 使用线程锁暂停签到线程
	 */
	private static void pauseThread()
	{
		synchronized (context) {
			try {
				context.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 给《一键签到》主程序发送广播，让其弹出验证码
	 * @param siteName 需要输入验证码的网站名称
	 * @param user 用户名
	 * @param reason 需要输入验证码的原因
	 */
	private static void sendShowCaptchaDialogBC(String siteName, String user, String reason)
	{
		Intent intent = new Intent(SRV_ACTION);
		intent.putExtra("cmd", CMD_THREAD_SHOW_CAPTCHA);
		intent.putExtra("siteName", siteName);
		intent.putExtra("user", user);
		intent.putExtra("reason", reason);
		context.sendBroadcast(intent);
		System.out.println("给Service发送广播，让其显示验证码");
	}
	
	
	
	
	
	
	
}
