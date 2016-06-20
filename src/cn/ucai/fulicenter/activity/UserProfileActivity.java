package cn.ucai.fulicenter.activity;

import java.io.ByteArrayOutputStream;
import java.io.File;

import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.toolbox.NetworkImageView;
import com.easemob.EMValueCallBack;

import cn.ucai.fulicenter.I;
import cn.ucai.fulicenter.R;
import cn.ucai.fulicenter.FuliCenterApplication;
import cn.ucai.fulicenter.applib.controller.HXSDKHelper;
import cn.ucai.fulicenter.DemoHXSDKHelper;
import cn.ucai.fulicenter.bean.Message;
import cn.ucai.fulicenter.bean.User;
import cn.ucai.fulicenter.data.ApiParams;
import cn.ucai.fulicenter.data.GsonRequest;
import cn.ucai.fulicenter.data.MultipartRequest;
import cn.ucai.fulicenter.data.RequestManager;
import cn.ucai.fulicenter.db.UserDao;
import cn.ucai.fulicenter.domain.EMUser;
import cn.ucai.fulicenter.listener.OnSetAvatarListener;
import cn.ucai.fulicenter.utils.ImageUtils;
import cn.ucai.fulicenter.utils.UserUtils;
import cn.ucai.fulicenter.utils.Utils;

import com.squareup.picasso.Picasso;

public class UserProfileActivity extends BaseActivity implements OnClickListener{
	
	private static final int REQUESTCODE_PICK = 1;
	private static final int REQUESTCODE_CUTTING = 2;
	private NetworkImageView headAvatar;
	private ImageView headPhotoUpdate;
	private ImageView iconRightArrow;
	private TextView tvNickName;
	private TextView tvUsername;
	private ProgressDialog dialog;
	private RelativeLayout rlNickName;
	UserProfileActivity mContext;
	OnSetAvatarListener mOnSetAvatarListener;

	
	
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		mContext=this;
		setContentView(R.layout.activity_user_profile);
		initView();
		initListener();
	}
	
	private void initView() {
		headAvatar = (NetworkImageView) findViewById(R.id.user_head_avatar);
		headPhotoUpdate = (ImageView) findViewById(R.id.user_head_headphoto_update);
		tvUsername = (TextView) findViewById(R.id.user_username);
		tvNickName = (TextView) findViewById(R.id.user_nickname);
		rlNickName = (RelativeLayout) findViewById(R.id.rl_nickname);
		iconRightArrow = (ImageView) findViewById(R.id.ic_right_arrow);
	}
	
	private void initListener() {
		Intent intent = getIntent();
		String username = intent.getStringExtra("username");
		String groupId = intent.getStringExtra("groupId");
		Log.e("main","UserProfileActivity："+username);
		boolean enableUpdate = intent.getBooleanExtra("setting", false);
		if (enableUpdate) {
			headPhotoUpdate.setVisibility(View.VISIBLE);
			iconRightArrow.setVisibility(View.VISIBLE);
			rlNickName.setOnClickListener(this);
			headAvatar.setOnClickListener(this);
		} else {
			headPhotoUpdate.setVisibility(View.GONE);
			iconRightArrow.setVisibility(View.INVISIBLE);
		}
		if (username == null || username.equals(FuliCenterApplication.getInstance().getUserName())) {
			tvUsername.setText(FuliCenterApplication.getInstance().getUserName());
			UserUtils.setCurrentUserBeanNick(tvNickName);
			UserUtils.setCurrentUserBeanAvatar(headAvatar);
		} else {
			if (groupId != null) {
				UserUtils.setGroupMenberNick(groupId, username, tvNickName);
				UserUtils.setUserAvatar(UserUtils.getAvatarPath(username),headAvatar);

			} else {

				UserUtils.setUserBeanNick(username, tvNickName);
				UserUtils.setUserBeanAvatar(username, headAvatar);
				//asyncFetchUserInfo(username);
			}
			tvUsername.setText(username);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.user_head_avatar:
			mOnSetAvatarListener = new OnSetAvatarListener(mContext, R.id.layout_user_profile,getAvatarName(),I.AVATAR_TYPE_USER_PATH);
			//uploadHeadPhoto();
			break;
		case R.id.rl_nickname:
			final EditText editText = new EditText(this);
			new Builder(this).setTitle(R.string.setting_nickname).setIcon(android.R.drawable.ic_dialog_info).setView(editText)
					.setPositiveButton(R.string.dl_ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							String nickString = editText.getText().toString();
							if (TextUtils.isEmpty(nickString)) {
								Toast.makeText(UserProfileActivity.this, getString(R.string.toast_nick_not_isnull), Toast.LENGTH_SHORT).show();
								return;
							}
							updateNickName(nickString);
						}
					}).setNegativeButton(R.string.dl_cancel, null).show();
			break;
		default:
			break;
		}

	}
	String avatarName;
	private String getAvatarName() {
		avatarName = System.currentTimeMillis()+"";
		return avatarName;
	}

	public void asyncFetchUserInfo(String username){
		((DemoHXSDKHelper) HXSDKHelper.getInstance()).getUserProfileManager().asyncGetUserInfo(username, new EMValueCallBack<EMUser>() {
			
			@Override
			public void onSuccess(EMUser user) {
				if (user != null) {
					tvNickName.setText(user.getNick());
					if(!TextUtils.isEmpty(user.getAvatar())){
						 Picasso.with(UserProfileActivity.this).load(user.getAvatar()).placeholder(R.drawable.default_avatar).into(headAvatar);
					}else{
						Picasso.with(UserProfileActivity.this).load(R.drawable.default_avatar).into(headAvatar);
					}
					UserUtils.saveUserInfo(user);
				}
			}
			
			@Override
			public void onError(int error, String errorMsg) {
			}
		});
	}
	
	
	
	private void uploadHeadPhoto() {
		Builder builder = new Builder(this);
		builder.setTitle(R.string.dl_title_upload_photo);
		builder.setItems(new String[] { getString(R.string.dl_msg_take_photo), getString(R.string.dl_msg_local_upload) },
				new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						switch (which) {
						case 0:
							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_no_support),
									Toast.LENGTH_SHORT).show();
							break;
						case 1:
							Intent pickIntent = new Intent(Intent.ACTION_PICK,null);
							pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
							startActivityForResult(pickIntent, REQUESTCODE_PICK);
							break;
						default:
							break;
						}
					}
				});
		builder.create().show();
	}

	private void updateNickName(String nickName){
		try {
			String path = new ApiParams()
							.with(I.User.USER_NAME, FuliCenterApplication.getInstance().getUserName())
                            .with(I.User.NICK,nickName)
							.getRequestUrl(I.REQUEST_UPDATE_USER_NICK);
			executeRequest(new GsonRequest<User>(path,User.class,responseUpdateNickname(nickName),errorListener()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Response.Listener<User> responseUpdateNickname(final String nickName) {
		return new Response.Listener<User>() {
			@Override
			public void onResponse(User user) {
				if(user!=null&&user.isResult()){
					updateRemoteNick(nickName);
				}else {
					Utils.showToast(mContext,Utils.getResourceString(mContext,user.getMsg()),Toast.LENGTH_SHORT);
				}
			}
		};
	}


	private void updateRemoteNick(final String nickName) {
		dialog = ProgressDialog.show(this, getString(R.string.dl_update_nick), getString(R.string.dl_waiting));
		new Thread(new Runnable() {

			@Override
			public void run() {
				boolean updatenick = ((DemoHXSDKHelper)HXSDKHelper.getInstance()).getUserProfileManager().updateParseNickName(nickName);
				if (UserProfileActivity.this.isFinishing()) {
					return;
				}
				if (!updatenick) {
					runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_fail), Toast.LENGTH_SHORT)
									.show();
							dialog.dismiss();
						}
					});
				} else {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							dialog.dismiss();
							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatenick_success), Toast.LENGTH_SHORT)
									.show();
							tvNickName.setText(nickName);
							FuliCenterApplication.currentUserNick = nickName;
							User user = FuliCenterApplication.getInstance().getUser();
							user.setMUserNick(nickName);
							UserDao dao = new UserDao(mContext);
							dao.updateUser(user);

						}
					});
				}
			}
		}).start();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		switch (requestCode) {
//		case REQUESTCODE_PICK:
//			if (data == null || data.getData() == null) {
//				return;
//			}
//			startPhotoZoom(data.getData());
//			break;
//		case REQUESTCODE_CUTTING:
//			if (data != null) {
//				setPicToView(data);
//			}
//			break;
//		default:
//			break;
//		}
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode!=RESULT_OK){
			return;
		}
		mOnSetAvatarListener.setAvatar(requestCode,data,headAvatar);
		if(requestCode==OnSetAvatarListener.REQUEST_CROP_PHOTO){
			dialog = ProgressDialog.show(this, getString(R.string.dl_update_photo), getString(R.string.dl_waiting));
			RequestManager.getRequestQueue().getCache().remove(UserUtils.getAvatarPath(
					FuliCenterApplication.getInstance().getUserName()
			));
			updateUserAvatar();
			dialog.show();
		}
	}
	private final String boundary = "apiclient-" + System.currentTimeMillis();
	private final String mimeType = "multipart/form-data;boundary=" + boundary;
	byte[] multipartBody;
	private Bitmap bitmap;
	private void updateUserAvatar() {

		File file = new File (ImageUtils.getAvatarPath(mContext,I.AVATAR_TYPE_USER_PATH),avatarName+I.AVATAR_SUFFIX_JPG);
		String  path = file.getAbsolutePath();
		bitmap = BitmapFactory.decodeFile(path);
		multipartBody = getImageBytes(bitmap);
		String url = null;
		try {
			url = new ApiParams()
                    .with(I.User.USER_NAME, FuliCenterApplication.getInstance().getUserName())
                    .with(I.AVATAR_TYPE,I.AVATAR_TYPE_USER_PATH)
                    .getRequestUrl(I.REQUEST_UPLOAD_AVATAR);
			executeRequest(new MultipartRequest<Message>(url,Message.class,null,
					responseUpdateAvatar(),errorListener(),mimeType,multipartBody));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Response.Listener<Message> responseUpdateAvatar() {
		return new Response.Listener<Message>() {
			@Override
			public void onResponse(Message message) {
				if(message.isResult()){
					RequestManager.getRequestQueue()
							.getCache().remove(UserUtils.
					getAvatarPath(FuliCenterApplication.getInstance().getUserName()));
					UserUtils.setCurrentUserBeanAvatar(headAvatar);
					Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_success),
							Toast.LENGTH_SHORT).show();

				}
				else {
					UserUtils.setCurrentUserBeanAvatar(headAvatar);
					Toast.makeText(UserProfileActivity.this,getString(R.string.toast_updatenick_fail),
							Toast.LENGTH_SHORT).show();
				}
				dialog.dismiss();
			}
		};
	}

	public byte[] getImageBytes(Bitmap bmp){
		if(bmp==null)return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.JPEG,100,baos);
		byte[] imageBytes = baos.toByteArray();
		return imageBytes;
	}

	public void startPhotoZoom(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", true);
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		intent.putExtra("outputX", 300);
		intent.putExtra("outputY", 300);
		intent.putExtra("return-data", true);
		intent.putExtra("noFaceDetection", true);
		startActivityForResult(intent, REQUESTCODE_CUTTING);
	}
	
	/**
	 * save the picture data
	 * 
	 * @param picdata
	 */
	private void setPicToView(Intent picdata) {
		Bundle extras = picdata.getExtras();
		if (extras != null) {
			Bitmap photo = extras.getParcelable("data");
			Drawable drawable = new BitmapDrawable(getResources(), photo);
			headAvatar.setImageDrawable(drawable);
			uploadUserAvatar(Bitmap2Bytes(photo));
		}

	}
	
	private void uploadUserAvatar(final byte[] data) {
		dialog = ProgressDialog.show(this, getString(R.string.dl_update_photo), getString(R.string.dl_waiting));
		new Thread(new Runnable() {

			@Override
			public void run() {
				final String avatarUrl = ((DemoHXSDKHelper)HXSDKHelper.getInstance()).getUserProfileManager().uploadUserAvatar(data);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dialog.dismiss();
						if (avatarUrl != null) {
							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_success),
									Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(UserProfileActivity.this, getString(R.string.toast_updatephoto_fail),
									Toast.LENGTH_SHORT).show();
						}

					}
				});

			}
		}).start();

		dialog.show();
	}
	
	
	public byte[] Bitmap2Bytes(Bitmap bm){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}
}
