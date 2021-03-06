/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2017 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface.OnShowListener;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.io.StorageHandler;
import org.catrobat.catroid.ui.WebViewActivity;
import org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment;
import org.catrobat.catroid.utils.DownloadUtil;
import org.catrobat.catroid.utils.ToastUtil;
import org.catrobat.catroid.utils.Utils;

import java.io.IOException;

public class OverwriteRenameMediaDialog extends DialogFragment implements OnClickListener {

	private static final String TAG = OverwriteRenameMediaDialog.class.getSimpleName();

	protected RadioButton replaceButton;
	protected RadioButton renameButton;
	protected String mediaName;
	protected String url;
	protected String mediaType;
	protected String filePath;
	protected String callingActivity;
	protected Context context;
	protected WebViewActivity webViewActivity;
	protected EditText mediaText;
	protected TextView mediaTextView;
	protected View mediaTextLine;

	public static final String DIALOG_FRAGMENT_TAG = "overwrite_rename_media";

	public OverwriteRenameMediaDialog() {
		super();
	}

	public void setMediaName(String mediaName) {
		this.mediaName = mediaName;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public void setMediaType(String type) {
		this.mediaType = type;
	}

	public void setFilePath(String path) {
		this.filePath = path;
	}

	public void setCallingActivity(String activity) {
		this.callingActivity = activity;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public void setWebViewActivity(WebViewActivity activity) {
		this.webViewActivity = activity;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		View dialogView = View.inflate(getActivity(), R.layout.dialog_overwrite_media, null);

		replaceButton = (RadioButton) dialogView.findViewById(R.id.dialog_overwrite_media_radio_replace);
		replaceButton.setOnClickListener(this);
		renameButton = (RadioButton) dialogView.findViewById(R.id.dialog_overwrite_media_radio_rename);
		renameButton.setOnClickListener(this);
		mediaText = (EditText) dialogView.findViewById(R.id.dialog_overwrite_media_edit);
		mediaText.setText(mediaName);
		mediaTextView = (TextView) dialogView.findViewById(R.id.dialog_overwrite_media_edit_text);
		mediaTextLine = dialogView.findViewById(R.id.dialog_overwrite_media_edit_line);

		final int header;
		final int replaceText;
		final int renameText;
		final int renameHeaderText;
		switch (mediaType) {
			case Constants.MEDIA_TYPE_LOOK:
				header = R.string.name_already_exists;
				replaceText = R.string.overwrite_replace_look;
				renameText = R.string.overwrite_rename_look;
				renameHeaderText = R.string.look_name_label;
				break;
			case Constants.MEDIA_TYPE_SOUND:
				header = R.string.name_already_exists;
				replaceText = R.string.overwrite_replace_sound;
				renameText = R.string.overwrite_rename_sound;
				renameHeaderText = R.string.sound_name_label;
				break;
			default:
				header = R.string.rename_sprite_dialog;
				replaceText = R.string.overwrite_replace_default;
				renameText = R.string.overwrite_rename_default;
				renameHeaderText = R.string.sprite_name_label;
		}

		replaceButton.setText(replaceText);
		renameButton.setText(renameText);
		mediaTextView.setText(renameHeaderText);

		Dialog dialog = new AlertDialog.Builder(getActivity()).setView(dialogView).setTitle(header)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dismiss();
					}
				}).create();

		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		dialog.setOnShowListener(new OnShowListener() {
			@Override
			public void onShow(final DialogInterface dialog) {
				Button positiveButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
				positiveButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						handleOkButton();
					}
				});
			}
		});

		dialog.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
					boolean okButtonResult = handleOkButton();
					if (!okButtonResult) {
						return false;
					} else {
						dismiss();
					}
					return okButtonResult;
				} else if (keyCode == KeyEvent.KEYCODE_BACK) {
					dismiss();
					return true;
				}
				return false;
			}
		});

		return dialog;
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.dialog_overwrite_media_radio_replace:
				mediaTextView.setVisibility(TextView.GONE);
				mediaTextLine.setVisibility(View.GONE);
				mediaText.setVisibility(EditText.GONE);
				break;

			case R.id.dialog_overwrite_media_radio_rename:
				mediaTextView.setVisibility(TextView.VISIBLE);
				mediaTextLine.setVisibility(View.VISIBLE);
				mediaText.setVisibility(EditText.VISIBLE);
				break;

			default:
				break;
		}
	}

	private boolean handleOkButton() {
		if (replaceButton.isChecked()) {
			switch (mediaType) {
				case Constants.MEDIA_TYPE_LOOK:
					for (LookData lookData : ProjectManager.getInstance().getCurrentSprite().getLookList()) {
						if (lookData.getName().compareTo(mediaName) == 0) {
							ProjectManager.getInstance().getCurrentSprite().getLookList().remove(lookData);
							try {
								StorageHandler.deleteFile(lookData.getAbsolutePath());
							} catch (IOException e) {
								Log.e(TAG, Log.getStackTraceString(e));
							}
							break;
						}
					}
					break;
				case Constants.MEDIA_TYPE_SOUND:
					for (SoundInfo soundInfo : ProjectManager.getInstance().getCurrentSprite().getSoundList()) {
						if (soundInfo.getName().compareTo(mediaName) == 0) {
							ProjectManager.getInstance().getCurrentSprite().getSoundList().remove(soundInfo);
							try {
								StorageHandler.deleteFile(soundInfo.getAbsolutePath());
							} catch (IOException e) {
								Log.e(TAG, Log.getStackTraceString(e));
							}
							break;
						}
					}
					break;
			}
			DownloadUtil.getInstance().startMediaDownload(context, url, mediaName, filePath);
		} else if (renameButton.isChecked()) {
			String newMediaName = mediaText.getText().toString();
			switch (mediaType) {
				case Constants.MEDIA_TYPE_LOOK:
					if (callingActivity.contains(LookListFragment.TAG)) {
						if (Utils.checkIfLookExists(newMediaName)) {
							ToastUtil.showError(context, R.string.name_already_exists);
							return false;
						}
					} else {
						return true;
					}
					break;
				case Constants.MEDIA_TYPE_SOUND:
					if (Utils.checkIfSoundExists(newMediaName)) {
						ToastUtil.showError(context, R.string.name_already_exists);
						return false;
					}
					break;
			}
			filePath = filePath.replace(mediaName, newMediaName);

			DownloadUtil.getInstance().startMediaDownload(context, url, newMediaName, filePath);
		}
		dismiss();

		return true;
	}
}
