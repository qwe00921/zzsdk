package com.zz.sdk.layout;

import java.lang.reflect.Constructor;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.zz.sdk.ParamChain;
import com.zz.sdk.ParamChain.KeyGlobal;
import com.zz.sdk.activity.LAYOUT_TYPE;
import com.zz.sdk.protocols.ActivityControlInterface;
import com.zz.sdk.util.Logger;

/**
 * 视图工厂
 * 
 * @author nxliao
 * @version v0.1.0.20130927
 */
public class LayoutFactory {

	public static final class KeyLayoutFactory implements KeyGlobal {
		protected static final String _TAG_ = KeyGlobal._TAG_
				+ "key_layout_factory" + _SEPARATOR_;

		/** 键：宿主, 类型 {@link ILayoutHost} */
		public static final String K_HOST = _TAG_ + "host";

		/** 键：视图, 类型 {@link ILayoutView} */
		public static final String K_VIEW = _TAG_ + "view";

		protected KeyLayoutFactory() {
		}

	}

	public static interface ILayoutHost {
		/**
		 * 返回上一界面
		 */
		public void back();

		/**
		 * 退出
		 */
		public void exit();

		/**
		 * 进入新界面
		 * 
		 * @param type 上下文
		 * @param rootEnv 环境变量
		 */
		public void enter(LAYOUT_TYPE type, ParamChain rootEnv);

		/**
		 * 进入新界面
		 * 
		 * @param classLoader
		 *            类加载器，null表示使用默认
		 * @param className
		 *            类名
		 * @param rootEnv
		 *            环境变量
		 */
		public void enter(ClassLoader classLoader, String className,
				ParamChain rootEnv);

		/**
		 * 设置窗体事件监听器，调用者自己维护生命周期
		 * 
		 * @param controlInterface 监听器
		 */
		public void addActivityControl(ActivityControlInterface controlInterface);

		public void removeActivityControl(
				ActivityControlInterface controlInterface);
	}

	public static interface ILayoutView {

		/***
		 * 进入，此时可启动初始化代码
		 * 
		 * @return 是否成功响应
		 */
		public boolean onEnter();

		/**
		 * @return 是否成功响应暂停
		 */
		public boolean onPause();

		/**
		 * @return 是否成功响应恢复
		 */
		public boolean onResume();

		/**
		 * 是否允许被关闭
		 * 
		 * @param isBack
		 *            true表示仅返回， false表示想要完全退出
		 * @return true 表示允许关闭， false 表示继续停留在这个界面
		 */
		public boolean isExitEnabled(boolean isBack);

		/**
		 * @return 是否成功响应被关闭
		 */
		public boolean onExit();

		/**
		 * @return 获取环境变量
		 */
		public ParamChain getEnv();

		/***
		 * 是否有效
		 * 
		 * @return 是否有效
		 */
		public boolean isAlive();

		/**
		 * 获取主视图，用于窗体显示
		 * 
		 * @return 主视图
		 */
		public View getMainView();
	}

	/**
	 * 创建 视图
	 * 
	 * @param ctx 上下文
	 * @param type 类型
	 * @param rootEnv 环境变量
	 * @return
	 */
	public static ILayoutView createLayout(Context ctx, LAYOUT_TYPE type,
			ParamChain rootEnv) {

		switch (type) {
		case LoginMain:
			return new LoginMainLayout(ctx, rootEnv);
		case PaymentList:
			return new PaymentListLayout(ctx, rootEnv);
		case Exchange:
			return new ExchangeLayout(ctx, rootEnv);
		default:
			break;
		}

		return null;
	}

	/**
	 * 构造指定类名
	 * 
	 * @param ctx
	 * @param className
	 *            类名
	 * @param classLoader
	 *            加载器，若为null表示使用虚拟机的默认类加载器
	 * @param rootEnv
	 * @return
	 */
	public static ILayoutView createLayout(Context ctx, String className,
			ClassLoader classLoader, ParamChain rootEnv) {
		try {
			Class<?> lFactoryClass = Class
					.forName(className, true, classLoader);
			if (ILayoutView.class.isAssignableFrom(lFactoryClass)) {
				Constructor<?> c = lFactoryClass.getConstructor(Context.class,
						ParamChain.class);
				return (ILayoutView) c.newInstance(ctx, rootEnv);
				// return (ILayoutView) lFactoryClass.newInstance();
			}
		} catch (Exception e) {
			Logger.d("Cannot instanciate layout [" + className + "]");
			e.printStackTrace();
		}
		return null;
	}
}
