package com.zz.sdk.util;

import com.zz.sdk.entity.PayChannel;

/**
 * @Description: 管理静态数据, <strong>内部类</strong>
 * @author roger
 */

public class Application {
	/**
	 * 用户指定的金额
	 */
	public static int changeCount;
	/**
	 * 当前正在登录的用户帐号(用户信息保存在数据库中
	 */
	public static String loginName;

	public static String password;

	private static String sGameUser;
	private static String sExLoginName;
	/**
	 * 支付渠道信息
	 */
	public static PayChannel[] mPayChannels;

	/**
	 * 是否登录
	 */
	public static boolean isLogin = false;

	/**
	 * 客服热线
	 * 
	 */
	public static String customerServiceHotline;

	/**
	 * 客服QQ
	 */
	public static String customerServiceQQ;

	public static String topicTitle;
	public static String topicDes;
	public static String cardAmount;
	/** 固定支付金额的通道索引, -1为空 */
	public static int staticAmountIndex;
	public static int payStatusCancel = 0;
	/** 冲值完成后是否关闭充值平台 */
	public static boolean isCloseWindow;
	public static int isAlreadyCB = 0;
	public static int isMessagePage = 0;
	public static boolean isDisplayLoginTip = false; // 是否显示登录提示
	public static boolean isDisplayLoginfail = false;// 是否显示登录失败提示

	private Application() {

	}

	public static synchronized String getLoginName() {
		return loginName;
	}

	public static synchronized void SetLoginName(String name) {
		isLogin = (name != null);
		loginName = name;
	}

	public static synchronized boolean hasExLoginName() {
		return sExLoginName != null;
	}

	public static synchronized String getExLoginName() {
		return sExLoginName == null ? loginName : sExLoginName;
	}

	/** 更新密码 */
	public static synchronized void updatePasswd(String newPasswd) {
		setLoginInfo(loginName, newPasswd, sGameUser, sExLoginName);
	}

	/**
	 * 设置用户登录信息
	 * 
	 * @param loginName
	 *            用户名
	 * @param passwd
	 *            密码
	 */
	public static synchronized void setLoginInfo(String loginName, String passwd) {
		setLoginInfo(loginName, passwd, null, null);
	}

	/**
	 * 设置用户的登录信息
	 * 
	 * @param loginName
	 *            用户名
	 * @param passwd
	 *            密码
	 * @param gameUser
	 *            游戏用户，一般情况下与 loginName 一致
	 * @param exLoginName
	 *            第三方登录名，一般情况下与 loginName 一致
	 */
	public static synchronized void setLoginInfo(String loginName,
			String passwd, String gameUser, String exLoginName) {
		Application.loginName = loginName;
		Application.password = passwd;
		if (sGameUser != gameUser)
			sGameUser = gameUser;
		if (sExLoginName != exLoginName)
			sExLoginName = exLoginName;
	}
}
