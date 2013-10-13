package com.zz.sdk.activity;

import java.util.Stack;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.zz.sdk.BuildConfig;
import com.zz.sdk.activity.ParamChain.KeyCaller;
import com.zz.sdk.activity.ParamChain.KeyGlobal;
import com.zz.sdk.activity.ParamChain.ValType;
import com.zz.sdk.layout.LAYOUT_TYPE;
import com.zz.sdk.layout.LayoutFactory;
import com.zz.sdk.layout.LayoutFactory.ILayoutView;
import com.zz.sdk.layout.LayoutFactory.KeyLayoutFactory;
import com.zz.sdk.protocols.ActivityControlInterface;
import com.zz.sdk.util.Logger;

/**
 * 基本窗体
 * 
 * @author nxliao
 * 
 */
public class BaseActivity extends Activity {

	/** 视图栈 */
	final private Stack<ILayoutView> mViewStack = new Stack<ILayoutView>();

	private String mName;

	private ParamChain mRootEnv;

	private Stack<ActivityControlInterface> mInterfacesStack;

	private static ParamChain ROOT_ENV;

	public static final synchronized ParamChain GET_GLOBAL_PARAM_CHAIN() {
		if (ROOT_ENV == null) {
			ROOT_ENV = ParamChainImpl.GLOBAL().grow(
					BaseActivity.class.getName());
		}
		return ROOT_ENV;
	}

	static final class KeyBaseActivity implements KeyGlobal {
		protected static final String _TAG_ = KeyGlobal._TAG_ + "base_activity"
				+ _SEPARATOR_;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prepare_activity(this);
		boolean init_success = init(this);
		if (!init_success)
			end();
	}

	protected void prepare_activity(Activity activity) {
		// Utils.loack_screen_orientation(activity);
		// activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	/**
	 * 初始化。
	 * 
	 * @param activity
	 *            窗体实例
	 * @return 是否成功
	 */
	protected boolean init(Activity activity) {
		ParamChain env = null;
		Intent intent = activity.getIntent();
		if (intent != null) {
			mName = intent.getStringExtra(KeyGlobal.K_UI_NAME);
			if (mName != null) {
				Object o = GET_GLOBAL_PARAM_CHAIN().getParent(
						BaseActivity.class.getName()).remove(mName);
				if (o instanceof ParamChain) {
					env = (ParamChain) o;
				}
			}
		}

		if (env == null) {
			if (BuildConfig.DEBUG) {
				Logger.e("找不到有效变量环境");
			}
			return false;
		}

		mRootEnv = env.grow();
		mRootEnv.add(KeyGlobal.K_UI_ACTIVITY, activity, ValType.TEMPORARY);
		mRootEnv.add(KeyLayoutFactory.K_HOST, new LayoutFactory.ILayoutHost() {
			@Override
			public void exit() {
				end();
			}

			@Override
			public void back() {
				popViewFromStack();
			}

			@Override
			public void enter(LAYOUT_TYPE type, ParamChain rootEnv) {
				tryEnterView(type, rootEnv);
			}

			@Override
			public void enter(ClassLoader classLoader, String className,
					ParamChain rootEnv) {
				tryEnterView(classLoader, className, rootEnv);
			}

			@Override
			public void addActivityControl(
					ActivityControlInterface controlInterface) {
				if (!mInterfacesStack.isEmpty()) {
					mInterfacesStack.remove(controlInterface);
				}
				mInterfacesStack.push(controlInterface);
			}

			@Override
			public void removeActivityControl(
					ActivityControlInterface controlInterface) {
				if (!mInterfacesStack.isEmpty())
					mInterfacesStack.remove(controlInterface);
			}

		}, ValType.TEMPORARY);

		mInterfacesStack = new Stack<ActivityControlInterface>();

		// 创建主视图
		LAYOUT_TYPE type = mRootEnv.get(KeyGlobal.K_UI_VIEW_TYPE,
				LAYOUT_TYPE.class);
		if (!tryEnterView(type, mRootEnv)) {
			Logger.e("bad root view");
			return false;
		}

		return true;
	}

	private boolean tryEnterView(LAYOUT_TYPE type, ParamChain rootEnv) {
		ILayoutView vl = LayoutFactory.createLayout(getBaseContext(), type,
				rootEnv);
		return tryEnterView(vl);
	}

	private boolean tryEnterView(ClassLoader classLoader, String className,
			ParamChain rootEnv) {
		ILayoutView vl = LayoutFactory.createLayout(getBaseContext(),
				className, classLoader, rootEnv);
		return tryEnterView(vl);
	}

	private boolean tryEnterView(ILayoutView vl) {
		if (vl != null) {
			pushView2Stack(vl);
			vl.onEnter();
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!mInterfacesStack.isEmpty()) {
			ActivityControlInterface aci = mInterfacesStack.peek();
			Boolean ret = aci.onKeyDownControl(keyCode, event);
			if (ret != null) {
				return ret;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!mInterfacesStack.isEmpty()) {
			ActivityControlInterface aci = mInterfacesStack.peek();
			if (aci.onActivityResultControl(requestCode, resultCode, data)) {
				return;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (!mInterfacesStack.isEmpty()) {
			ActivityControlInterface aci = mInterfacesStack.peek();
			if (aci.onConfigurationChangedControl(newConfig)) {
				return;
			}
		}

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// Nothing need to be done here
		} else {
			// Nothing need to be done here
		}
	}

	@Override
	public void onBackPressed() {
		if (!mInterfacesStack.isEmpty()) {
			ActivityControlInterface aci = mInterfacesStack.peek();
			if (aci.onBackPressedControl()) {
				return;
			}
		}

		if (popViewFromStack() != null)
			return;
		super.onBackPressed();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (BuildConfig.DEBUG) {
			System.out.println("销毁掉了 " + mName);
		}
		clean();
	}

	protected void clean() {
		if (mViewStack != null && !mViewStack.isEmpty()) {
			ILayoutView lv; // = mViewStack.pop();
			while (!mViewStack.isEmpty()) {
				lv = mViewStack.pop();
				if (lv.isAlive()) {
					lv.onExit();
				}
			}
			// mViewStack.clear();
		}
		mName = null;
		if (mRootEnv != null) {
			mRootEnv.autoRelease();
			mRootEnv = null;
		}
	}

	protected void pushView2Stack(ILayoutView vl) {
		if (mViewStack.size() > 0) {
			ILayoutView top = mViewStack.peek();
			if (top.isAlive()) {
				View peek = top.getRootView();
				peek.clearFocus();
				top.onPause();
				// peek.startAnimation(mAnimLeftOut);
			}
		}
		mViewStack.push(vl);
		View curView = vl.getRootView();
		setContentView(curView);
		curView.requestFocus();
		if (mViewStack.size() > 1) {
			// 启动动画
			// newView.startAnimation(mAnimRightIn);
		}
	}

	private View popViewFromStack() {
		if (mViewStack.size() > 1) {
			Boolean isCloseWindow = mRootEnv.get(KeyCaller.K_IS_CLOSE_WINDOW,
					Boolean.class);
			// if (Application.isCloseWindow && Application.isAlreadyCB == 1) {
			if (Boolean.TRUE.equals(isCloseWindow)) {
				this.finish();
				return null;
			}
			ILayoutView lv;

			// 弹出旧ui
			lv = mViewStack.peek();
			if (lv.isAlive()) {
				// 先判断是否允许关闭
				View pop = lv.getRootView();
				if (pop == null || lv.isExitEnabled()) {
					lv.onExit();
				} else {
					return pop;
				}
				if (pop != null) {
					pop.clearFocus();
				}
				// if (pop instanceof SmsChannelLayout) {
				// Application.isMessagePage = 1;
				// }
				// if (pop instanceof ChargeSMSDecLayout) {
				// Application.isMessagePage = 0;
				// }
				// if (Application.isMessagePage == 1 && isSendMessage == false)
				// {
				// // 短信取消后发送取消支付请求
				// Application.isMessagePage = 0;
				// smsPayCallBack(-2, null);
				//
				// }
			}
			lv = mViewStack.pop();

			lv = mViewStack.peek();
			View curView = lv.getRootView();
			setContentView(curView);
			curView.requestFocus();
			lv.onResume();

			return curView;
		} else {
			Logger.d("ChargeActivity exit");
			// if (Application.isAlreadyCB == 1) {
			// allPayCallBack(-2);
			// Application.isAlreadyCB = 0;
			// }
			finish();
			return null;
		}
	}

	protected void end() {
		if (mViewStack != null && mViewStack.size() > 0) {
			// 关闭前先判断是否允许关闭
			ILayoutView lv = mViewStack.peek();
			if (lv.isAlive() && !lv.isExitEnabled()) {
				return;
			}
		}

		finish();
	}
}
