package com.zz.sdk.layout;

import java.util.regex.Pattern;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zz.lib.pojo.PojoUtils;
import com.zz.sdk.LoginCallbackInfo;
import com.zz.sdk.MSG_STATUS;
import com.zz.sdk.MSG_TYPE;
import com.zz.sdk.ParamChain;
import com.zz.sdk.ParamChain.KeyUser;
import com.zz.sdk.ZZSDKConfig;
import com.zz.sdk.entity.result.BaseResult;
import com.zz.sdk.entity.result.ResultChangePwd;
import com.zz.sdk.entity.result.ResultLogin;
import com.zz.sdk.util.BitmapCache;
import com.zz.sdk.util.Constants;
import com.zz.sdk.util.Loading;
import com.zz.sdk.util.Logger;
import com.zz.sdk.util.ResConstants.Config.ZZDimen;
import com.zz.sdk.util.ResConstants.ZZStr;
import com.zz.sdk.util.UserUtil;
import com.zz.sdk.util.Utils;

/**
 * 登录主界面
 * <ul>
 * 未处理
 * <li>360用户
 * <li>逗趣用户
 * </ul>
 * 
 * @author nxliao
 * 
 */
class LoginMainLayout extends BaseLayout {

	/** 用户数据处理 */
	private UserUtil mUserUtil;
	/** 用户名 */
	private String mLoginName;
	/** 用户密码 */
	private String mPassword;
	/** 登录成功时显示提示 */
	private boolean mTipSuccess;
	/** 登录失败时显示提示 */
	private boolean mTipFailed;
	/** 登录状态 */
	private int mLoginState;

	private boolean mLoginForModify;

	private AutoLoginDialog mAutoDialog;
	private FrameLayout main;
	private Handler mHandler = new Handler();
	private Context ctx;
	private LinearLayout content;
	private String mSdkUserId;
	private String mNewPassword;

	protected static enum IDC implements IIDC {
		ACT_ERR,

		ACT_NORMAL,

		ACT_LOGIN,

		ACT_RIGHSTER,

		ACT_MODIFY_PASSWORD,

		BT_REGISTER,

		BT_LOGIN, BT_QUICK_LOGIN, BT_UPDATE_PASSWORD, RG_ACCOUNT_TYPE, BT_BACK,

		/** 修改密码·确认按钮 */
		BT_MODIFY_CONFIRM,

		/** 修改密码·新密码输入框 */
		ED_NEW_PASSOWRD,

		/** 登录·账号输入框 */
		ED_LOGIN_NAME,

		/** 登录·密码输入框 */
		ED_LOGIN_PASSWORD,

		/** 自动登录提示框的取消按钮 */
		BT_AUTO_LOGIN_CANCEL,

		_MAX_;

		protected final static int __start__ = BaseLayout.IDC._MAX_.id();

		public final int id() {
			return ordinal() + __start__;
		}

		/** 从 id 反查，如果失败则返回 {@link #_MAX_} */
		public final static IDC fromID(int id) {
			id -= __start__;
			if (id >= 0 && id < _MAX_.ordinal()) {
				return values()[id];
			}
			return _MAX_;
		}

	}

	public LoginMainLayout(Context context, ParamChain env) {
		super(context, env);
		this.ctx = context;
		initUI(context);
	}

	@Override
	protected void onInitEnv(Context ctx, ParamChain env) {
		mLoginState = MSG_STATUS.EXIT_SDK;

		mUserUtil = UserUtil.getInstance(ctx);
		mLoginName = env.get(KeyUser.K_LOGIN_NAME, String.class);
		mPassword = env.get(KeyUser.K_PASSWORD, String.class);
		if (mLoginName == null) {
			mLoginName = mUserUtil.getCachedLoginName();
			mPassword = mUserUtil.getCachedPassword();
		}

		// TODO:
		mTipSuccess = mTipFailed = true;
	}

	/**
	 * 切换活动面板，目前仅直接替换视图
	 * 
	 * @param act
	 */
	private void switchPanle(IDC act) {
		switch (act) {
		case ACT_MODIFY_PASSWORD: {
			main.removeAllViews();
			main.addView(createView_modifyPasswd(ctx));
		}
			break;

		default:
			break;
		}

	}

	private void onLoginSuccess() {
		mLoginState = MSG_STATUS.SUCCESS;
		removeExitTrigger();
		callHost_back();

		// 同步缓存

		ParamChain env = getEnv().getParent(KeyUser.class.getName());
		env.add(KeyUser.K_LOGIN_NAME, mLoginName);
		env.add(KeyUser.K_PASSWORD, mPassword);
		env.add(KeyUser.K_SDKUSER_ID, mSdkUserId);
		env.add(KeyUser.K_LOGIN_STATE_SUCCESS, Boolean.TRUE);

		mUserUtil.syncSdkUser();
	}

	@Override
	protected void clean() {
		if (mLoginState != MSG_STATUS.EXIT_SDK) {
			nofityLoginResult(getEnv(), mLoginState);
		}
		// 发出退出消息
		notifyCaller(MSG_TYPE.LOGIN, MSG_STATUS.EXIT_SDK, null);
	}

	/**
	 * 通知登录结果到回调函数
	 * 
	 * @param env
	 * @param state
	 */
	private void nofityLoginResult(ParamChain env, int state) {
		int code;
		switch (state) {
		case MSG_STATUS.SUCCESS:
			code = LoginCallbackInfo.STATUS_SUCCESS;
			break;
		case MSG_STATUS.FAILED:
			code = LoginCallbackInfo.STATUS_FAILURE;
			break;
		case MSG_STATUS.CANCEL:
		default:
			code = LoginCallbackInfo.STATUS_CLOSE_VIEW;
			break;
		}

		LoginCallbackInfo info = new LoginCallbackInfo();
		info.statusCode = code;
		info.loginName = mLoginName;
		info.sdkuserid = mSdkUserId;

		notifyCaller(MSG_TYPE.LOGIN, state, info);
	}

	@Override
	public boolean onEnter() {
		boolean ret = super.onEnter();
		if (ret) {
			// 将默认状态置为 “cancel”
			mLoginState = MSG_STATUS.CANCEL;

			checkAutoLogin();
		}
		return ret;
	}

	private void checkAutoLogin() {
		if (mLoginName != null && mPassword != null) {
			// show_auto_login_wait();
		}
	}

	@Override
	public void onClick(View v) {
		IDC idc = IDC.fromID(v.getId());
		switch (idc) {
		case BT_AUTO_LOGIN_CANCEL:
			break;

		// 注册账号
		case BT_REGISTER:
			main.removeAllViews();
			main.addView(createView_regist(ctx));
			break;

		// 修改密码
		case BT_UPDATE_PASSWORD:
			mLoginForModify = idc == IDC.BT_UPDATE_PASSWORD;
			// 登录
		case BT_LOGIN: {
			Pair<View, String> err = checkLoginInput();
			if (err == null)
				tryLoginWait(mLoginName, mPassword);
			else {
				showToast(err.second);
			}
		}
			break;

		case BT_MODIFY_CONFIRM: {
			Pair<View, String> err = checkModifyInput();
			if (err == null)
				tryModifyWait(mLoginName, mPassword, mNewPassword);
			else {
				showToast(err.second);
			}
		}
			break;

		// 快速登录
		case BT_QUICK_LOGIN:

			break;
		// 返回
		case BT_BACK:
			main.removeAllViews();
			main.addView(createView_login(ctx, true));
			break;
		default:
			super.onClick(v);
		}
	}

	/**
	 * 检查登录的输入内容是否合法。
	 * 
	 * @return <ul>
	 *         <li>如果通过检查，则更新变量 {@link #mLoginName} 和 {@link #mPassword}，并返回
	 *         null 。
	 *         <li>否则返回<出错View, 提示文本>
	 *         </ul>
	 */
	private Pair<View, String> checkLoginInput() {
		Pair<View, String> ret;

		View vLoginName = null;
		View vPassword = null;
		String loginName = get_child_text(IDC.ED_LOGIN_NAME);
		String password = get_child_text(IDC.ED_LOGIN_PASSWORD);
		do {
			Pair<Boolean, String> resultName = null;
			if (ZZSDKConfig.SUPPORT_DOUQU_LOGIN) {
				if (PojoUtils.isDouquUser(loginName)
						|| PojoUtils.isCMGEUser(loginName)) {
					resultName = new Pair<Boolean, String>(true, loginName);
				}
			}
			if (resultName == null)
				resultName = validUserName(loginName);
			if (!resultName.first) {
				// 输入不合法
				ret = new Pair<View, String>(vLoginName, resultName.second);
				break;
			}

			Pair<Boolean, String> resultPW = null;
			if (ZZSDKConfig.SUPPORT_DOUQU_LOGIN) {
				if (PojoUtils.isDouquUser(loginName)) {
					String desc = PojoUtils.isDouquPasswd(password);
					resultPW = new Pair<Boolean, String>(desc == null, desc);
				}
			}
			if (resultPW == null)
				resultPW = validPassWord(password);
			if (!resultPW.first) {
				ret = new Pair<View, String>(vPassword, resultPW.second);
				break;
			}

			// success
			mLoginName = loginName;
			mPassword = password;
			ret = null;
		} while (false);

		return ret;
	}

	/** 尝试登录 */
	private void tryLoginWait(String loginName, String password) {
		showPopup_Wait("正在登录……", new SimpleWaitTimeout() {
			public void onTimeOut() {
				onLoginTimeout();
			}
		});
		setExitTrigger(-1, "正在登录……");

		ITaskCallBack cb = new ITaskCallBack() {
			@Override
			public void onResult(AsyncTask<?, ?, ?> task, Object token,
					BaseResult result) {
				if (isCurrentTaskFinished(task)) {
					onLoginReuslt(result);
				}
			}
		};
		AsyncTask<?, ?, ?> task = LoginTask.createAndStart(mUserUtil, cb, this,
				loginName, password);
		setCurrentTask(task);
	}

	private void resetExitTrigger() {
		setExitTrigger(-1, null);
	}

	/** 登录超时 */
	private void onLoginTimeout() {
		resetExitTrigger();
		showPopup_Tip(ZZStr.CC_TRY_CONNECT_SERVER_TIMEOUT);
	}

	/**
	 * 处理登录结果
	 * 
	 * @param result
	 *            登录结果，成功或失败
	 */
	private void onLoginReuslt(BaseResult result) {
		if (result.isSuccess()) {
			if (mLoginForModify) {
				// 如果是为修改密码而登录
				hidePopup();
				resetExitTrigger();
				switchPanle(IDC.ACT_MODIFY_PASSWORD);
			} else {
				onLoginSuccess();
			}
		} else {
			if (result.isUsed()) {
				showPopup_Tip(result.getErrDesc());
			} else
				showPopup_Tip(ZZStr.CC_TRY_CONNECT_SERVER_FAILED);
		}
	}

	// ////////////////////////////////////////////////////////////////////////
	//
	// - modify password -
	//

	private Pair<View, String> checkModifyInput() {
		Pair<View, String> ret = null;
		View vPassword = null;
		String password = get_child_text(IDC.ED_NEW_PASSOWRD);
		do {
			Pair<Boolean, String> resultPW = null;
			if (ZZSDKConfig.SUPPORT_DOUQU_LOGIN) {
				if (PojoUtils.isDouquUser(mLoginName)) {
					String desc = PojoUtils.isDouquPasswd(password);
					resultPW = new Pair<Boolean, String>(desc == null, desc);
				}
			}
			if (resultPW == null)
				resultPW = validPassWord(password);
			if (!resultPW.first) {
				ret = new Pair<View, String>(vPassword, resultPW.second);
				break;
			}
			// success
			mNewPassword = password;
			ret = null;
		} while (false);

		return ret;
	}

	private void tryModifyWait(String loginName, String password,
			String newPasswd) {
		showPopup_Wait("正在修改密码……", new SimpleWaitTimeout() {
			public void onTimeOut() {
				onModifyTimeout();
			}
		});
		setExitTrigger(-1, "正在修改密码……");
		ITaskCallBack cb = new ITaskCallBack() {
			@Override
			public void onResult(AsyncTask<?, ?, ?> task, Object token,
					BaseResult result) {
				if (isCurrentTaskFinished(task)) {
					onModifyReuslt(result);
				}
			}
		};
		AsyncTask<?, ?, ?> task = ModifyPasswordTask.createAndStart(mUserUtil,
				cb, this, loginName, password, newPasswd);
		setCurrentTask(task);
	}

	protected void onModifyReuslt(BaseResult result) {
		if (result.isSuccess()) {
			mPassword = mNewPassword;
			onLoginSuccess();
		} else {
			if (result.isUsed()) {
				showPopup_Tip(result.getErrDesc());
			} else
				showPopup_Tip(ZZStr.CC_TRY_CONNECT_SERVER_FAILED);
		}
	}

	protected void onModifyTimeout() {
		resetExitTrigger();
		showPopup_Tip(ZZStr.CC_TRY_CONNECT_SERVER_TIMEOUT);
	}

	/**
	 * 服务器请求
	 */
	private void doPost() {

	}

	/**
	 * 服务器返回
	 */
	private void doBack() {

	}

	/**
	 * 创建登录 LinearLayout
	 * 
	 * @param ctx
	 * @param hasAccount
	 *            是否为第一次登录
	 * @return
	 */
	private LinearLayout createView_login(Context ctx, boolean hasAccount) {
		LoginLayout login = new LoginLayout(ctx, this, hasAccount);
		login.setAccount(mLoginName);
		login.setPassWord(mPassword);
		return login;
	}

	/**
	 * 创建修改密码LinearLayout
	 * 
	 * @param ctx
	 * @return
	 */
	private View createView_modifyPasswd(Context ctx) {
		LoginUpdatePwdLayout update = new LoginUpdatePwdLayout(ctx, this);
		update.setOldPassWord(mPassword);
		return update;
	}

	/**
	 * 创建注册LinearLayout
	 * 
	 * @param ctx
	 * @return
	 */
	private LinearLayout createView_regist(Context ctx) {
		LoginRegisterLayout reg = new LoginRegisterLayout(ctx, this);
		return reg;
	}

	/**
	 * 显示自动游戏登录Dialog
	 */
	private void show_auto_login_wait() {
		mAutoDialog = new AutoLoginDialog(getActivity());
		// 显示
		mAutoDialog.show();
		// 2秒
		mHandler.postDelayed(doAutoLogin, 2 * 1000);
	}

	protected void onInitUI(Context ctx) {
		set_child_visibility(BaseLayout.IDC.ACT_TITLE, VISIBLE);

		FrameLayout rv = getSubjectContainer();
		final boolean isVertical = Utils.isOrientationVertical(getContext());
		int widthPixels = getResources().getDisplayMetrics().widthPixels;
		int heightPixels = getResources().getDisplayMetrics().heightPixels;
		int weight1 = widthPixels * 4 / 5;

		int weight2 = widthPixels * (isVertical ? 8 : 7) / 8;

		setOrientation(VERTICAL);
		// 整体背景图
		rv.setBackgroundDrawable(BitmapCache.getDrawable(ctx,
				(isVertical ? Constants.ASSETS_RES_PATH_VERTICAL
						: Constants.ASSETS_RES_PATH) + "bj.jpg"));
		setWeightSum(1.0f);

		LinearLayout layout1 = new LinearLayout(ctx);
		layout1.setOrientation(HORIZONTAL);
		// layout1.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
		LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(weight1,
				0);
		lp1.weight = 0.27f;
		addView(layout1, lp1);

		ImageView logo = new ImageView(ctx);
		// logo.setImageDrawable(BitmapCache.getDrawable(mActivity,
		// Constants.ASSETS_RES_PATH + "logo.png"));
		LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(-2, -2);
		layout1.addView(logo, lpLogo);

		LinearLayout layout2 = new LinearLayout(ctx);
		LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(weight2,
				0);
		lp2.weight = 0.73f;
		addView(layout2, lp2);
		layout2.setGravity(Gravity.RIGHT);

		FrameLayout top = new FrameLayout(ctx);
		ImageView image = new ImageView(ctx);
		image.setImageDrawable(BitmapCache.getDrawable(ctx,
				Constants.ASSETS_RES_PATH + "logo2.png"));
		top.addView(image);
		FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		rv.addView(top, l);
		boolean hasAccount = mLoginName != null && mLoginName.length() > 0;

		main = new FrameLayout(ctx);
		LinearLayout login = createView_login(ctx, hasAccount);
		main.addView(login);
		rv.addView(main);
		// 显示“自动登录”框
		if (hasAccount) {
			show_auto_login_wait();
		}
	}

	private Runnable doAutoLogin = new Runnable() {
		@Override
		public void run() {
			// 先判断是否已经被cancel
			if (mAutoDialog.isShowing()) {
				try {
					// 取消显示
					mAutoDialog.cancel();
				} catch (Exception e) {
					Logger.d(e.getClass().getName());
				}

				// 模拟用户按下登陆按钮
				// onClick(btnLogin);
			}
		}
	};

	/**
	 * 自动登陆显示进度框
	 */
	class AutoLoginDialog extends Dialog {
		Context ctx;
		private Button cancel;

		public AutoLoginDialog(Context context) {
			super(context);
			this.ctx = context;

			getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			getWindow().setBackgroundDrawable(
					new ColorDrawable(Color.TRANSPARENT));
			LinearLayout content = new LinearLayout(ctx);
			// 垂直
			content.setOrientation(VERTICAL);
			content.setGravity(Gravity.CENTER_HORIZONTAL);
			content.setPadding(ZZDimen.dip2px(20), ZZDimen.dip2px(15),
					ZZDimen.dip2px(20), ZZDimen.dip2px(15));
			content.setBackgroundDrawable(Utils.getDrawable(ctx,
					"login_bg_03.png"));

			// 文字
			TextView tv = new TextView(ctx);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			tv.setTextColor(0xfffeef00);
			tv.setText("剩下2秒自动登陆游戏");
			tv.setTextSize(18);
			//
			Loading loading = new Loading(ctx);

			cancel = new Button(ctx);
			cancel.setId(IDC.BT_AUTO_LOGIN_CANCEL.id());
			cancel.setBackgroundDrawable(Utils.getStateListDrawable(ctx,
					"quxiao1.png", "quxiao.png"));
			cancel.setOnClickListener(LoginMainLayout.this);

			content.addView(tv);
			LinearLayout.LayoutParams lploading = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lploading.topMargin = ZZDimen.dip2px(10);
			content.addView(loading, lploading);
			LinearLayout.LayoutParams lpcancel = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lpcancel.topMargin = ZZDimen.dip2px(10);
			content.addView(cancel, lpcancel);

			// 对话框的内容布局
			setContentView(content);
			setCanceledOnTouchOutside(false);

		}

	}

	private static class LoginTask extends AsyncTask<Object, Void, ResultLogin> {
		protected static AsyncTask<?, ?, ?> createAndStart(UserUtil uu,
				ITaskCallBack callback, Object token, String loginName,
				String password) {
			LoginTask task = new LoginTask();
			task.execute(uu, callback, token, loginName, password);
			return task;
		}

		private ITaskCallBack mCallback;
		private Object mToken;

		@Override
		protected ResultLogin doInBackground(Object... params) {
			UserUtil uu = (UserUtil) params[0];
			ITaskCallBack callback = (ITaskCallBack) params[1];
			Object token = params[2];

			String loginName = (String) params[3];
			String password = (String) params[4];

			ResultLogin ret = uu.login(loginName, password);
			if (!this.isCancelled()) {
				mCallback = callback;
				mToken = token;
			}
			return ret;
		}

		@Override
		protected void onPostExecute(ResultLogin result) {
			if (mCallback != null) {
				mCallback.onResult(this, mToken, result);
			}
			mCallback = null;
			mToken = null;
		}
	}

	private static class ModifyPasswordTask extends
			AsyncTask<Object, Void, ResultChangePwd> {
		protected static AsyncTask<?, ?, ?> createAndStart(UserUtil uu,
				ITaskCallBack callback, Object token, String loginName,
				String password, String newPasswd) {
			ModifyPasswordTask task = new ModifyPasswordTask();
			task.execute(uu, callback, token, loginName, password, newPasswd);
			return task;
		}

		private ITaskCallBack mCallback;
		private Object mToken;

		@Override
		protected ResultChangePwd doInBackground(Object... params) {
			UserUtil uu = (UserUtil) params[0];
			ITaskCallBack callback = (ITaskCallBack) params[1];
			Object token = params[2];

			String loginName = (String) params[3];
			String password = (String) params[4];
			String newPasswd = (String) params[5];

			ResultChangePwd ret = uu.modifyPassword(loginName, password,
					newPasswd);
			if (!this.isCancelled()) {
				mCallback = callback;
				mToken = token;
			}
			return ret;
		}

		@Override
		protected void onPostExecute(ResultChangePwd result) {
			if (mCallback != null) {
				mCallback.onResult(this, mToken, result);
			}
			mCallback = null;
			mToken = null;
		}
	}

	/**
	 * 验证用户名输入
	 * 
	 * @param user
	 * @return
	 */
	private static Pair<Boolean, String> validUserName(String user) {
		String des = null;
		boolean result = false;
		if (user != null) {
			user = user.trim();
		}
		if (user == null || user.length() < 6) {
			des = "帐号长度至少6位";
		} else if (!user.matches("^(?!_)(?!.*?_$)[a-zA-Z0-9_]+$")) {
			des = "帐号必须由字母、数字或下划线组成,并以数字或字母开头";
			if (ZZSDKConfig.SUPPORT_DOUQU_LOGIN) {
				des += "；\r或使用 CMGE 通行证登录";
			}
		} else if (user.length() > 45) {
			des = "账号长度不能超过45位";
		} else {
			result = true;
		}
		Pair<Boolean, String> p = new Pair<Boolean, String>(result, des);
		return p;
	}

	/**
	 * 验证密码输入
	 * 
	 * @param pw
	 * @return
	 */
	private static Pair<Boolean, String> validPassWord(String pw) {
		String des = null;
		boolean result = false;
		if (pw != null) {
			pw = pw.trim();
		}
		if (pw == null || pw.length() < 6) {
			des = "密码长度至少6位";
		} else if (getChinese(pw)) {
			des = "密码不能包含中文";
		} else if (!pw.matches("^(?!_)(?!.*?_$)[a-zA-Z0-9]+$")) {
			des = "密码中只能包含数字和字母";
		} else if (pw.length() > 45) {
			des = "密码长度不能超过45位";
		} else {
			result = true;
		}
		Pair<Boolean, String> p = new Pair<Boolean, String>(result, des);
		return p;
	}

	/**
	 * @param str
	 * @return true表示包含有中文
	 */
	private static boolean getChinese(String str) {
		boolean HasChinese = false;
		if (str == null || "".equals(str.trim())) {
			return false;
		}
		char[] pwd = str.toCharArray();
		for (int i = 0; i < pwd.length; i++) {
			char c = pwd[i];
			if (Pattern.matches("[\u4e00-\u9fa5]", String.valueOf(c))) {
				HasChinese = true;
				break;
			}
		}
		return HasChinese;
	}
}
