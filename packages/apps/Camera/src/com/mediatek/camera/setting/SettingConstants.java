/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2014. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.camera.setting;

//TODO: can not reference the FeatureSwitcher under Google packages
import com.android.camera.FeatureSwitcher;

//TODO: need to be changed as xml
public class SettingConstants {
    private static final String TAG = " SettingConstants";
    
    public static final int UNKNOWN = -1;
    public static final String DEFAULT_CONINUOUS_CAPTURE_NUM = "20";
    public static final String KEY_VERSION                          = "pref_version_key";
    public static final String KEY_LOCAL_VERSION                    = "pref_local_version_key";
    
    public static final int SETTING_COUNT = 63;
    // setting key
    public static final String KEY_CAMERA_ID                 = "pref_camera_id_key";
    public static final String KEY_JPEG_QUALITY              = "pref_camera_jpegquality_key";
    public static final String KEY_RECORD_LOCATION           = "pref_camera_recordlocation_key";
    public static final String KEY_VIDEO_QUALITY             = "pref_video_quality_key";
    public static final String KEY_SLOW_MOTION_VIDEO_QUALITY = "pref_slow_motion_video_quality_key";
    public static final String KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL = "pref_video_time_lapse_frame_interval_key";
    public static final String KEY_PICTURE_SIZE              = "pref_camera_picturesize_key";
    public static final String KEY_FLASH                     = "pref_camera_flashmode_key";
    public static final String KEY_WHITE_BALANCE             = "pref_camera_whitebalance_key";
    public static final String KEY_SCENE_MODE                = "pref_camera_scenemode_key";
    public static final String KEY_EXPOSURE                  = "pref_camera_exposure_key";
    public static final String KEY_ISO                       = "pref_camera_iso_key";
    public static final String KEY_COLOR_EFFECT              = "pref_camera_coloreffect_key";
    public static final String KEY_CAMERA_ZSD                = "pref_camera_zsd_key";
    public static final String KEY_STEREO3D_PICTURE_SIZE     = "pref_camera_picturesize_stereo3d_key";
    public static final String KEY_STEREO3D_MODE             = "pref_stereo3d_mode_key";
    public static final String KEY_STEREO3D_PICTURE_FORMAT   = "pref_camera_pictureformat_key";
    public static final String KEY_VIDEO_RECORD_AUDIO        = "pref_camera_recordaudio_key";
    public static final String KEY_VIDEO_HD_AUDIO_RECORDING  = "pref_camera_video_hd_recording_key";
    public static final String KEY_CAMERA_AIS                = "perf_camera_ais_key";
    public static final String KEY_IMAGE_PROPERTIES          = "pref_camera_image_properties_key";//virtual item
    public static final String KEY_EDGE                      = "pref_camera_edge_key";
    public static final String KEY_HUE                       = "pref_camera_hue_key";
    public static final String KEY_SATURATION                = "pref_camera_saturation_key";
    public static final String KEY_BRIGHTNESS                = "pref_camera_brightness_key";
    public static final String KEY_CONTRAST                  = "pref_camera_contrast_key";
    public static final String KEY_SELF_TIMER                = "pref_camera_self_timer_key";
    public static final String KEY_ANTI_BANDING              = "pref_camera_antibanding_key";
    public static final String KEY_VIDEO_EIS                 = "pref_video_eis_key";
    public static final String KEY_VIDEO_3DNR                = "pref_video_3dnr_key";
    public static final String KEY_CONTINUOUS_NUMBER         = "pref_camera_shot_number";
    public static final String KEY_DUAL_CAMERA_MODE          = "pref_dual_camera_key";//virtual item
    public static final String KEY_FAST_AF                   = "pref_fast_af_key";
    public static final String KEY_DISTANCE                  = "pref_distance_key";
    public static final String KEY_PICTURE_RATIO             = "pref_camera_picturesize_ratio_key";
    public static final String KEY_VOICE                     = "pref_voice_key";
    public static final String KEY_SLOW_MOTION               = "pref_slow_motion_key";
    public static final String KEY_FACE_BEAUTY_PROPERTIES    = "pref_camera_facebeauty_properties_key";//virtual item
    public static final String KEY_FACE_BEAUTY_SMOOTH        = "pref_facebeauty_smooth_key";
    public static final String KEY_FACE_BEAUTY_SKIN_COLOR    = "pref_facebeauty_skin_color_key";
    public static final String KEY_FACE_BEAUTY_SHARP         = "pref_facebeauty_sharp_key";
    public static final String KEY_MULTI_FACE_BEAUTY         = "pref_face_beauty_multi_mode_key";
    public static final String KEY_CAMERA_FACE_DETECT        = "pref_face_detect_key";
    public static final String KEY_CAMERA_MODE               = "camera_mode_key";
    public static final String KEY_CAPTURE_MODE              = "capture_mode_key";
    public static final String KEY_RECORDING_HIHT            = "recoding_hint_key";
    public static final String KEY_MUTE_RECORDING_SOUND      = "mute_recoding_sound_key";
    public static final String KEY_FACE_BEAUTY_SLIM           = "pref_facebeauty_slim_key";
    public static final String KEY_FACE_BEAUTY_BIG_EYES       = "pref_facebeauty_big_eyes_key";
    public static final String KEY_FACE_BEAUTY               = "face_beauty_key";
    public static final String KEY_PANORAMA                  = "panorama_key";
    public static final String KEY_MAV                       = "mav_key";
    public static final String KEY_MOTION_TRACK              = "motion_track_key";
    public static final String KEY_HDR                       = "pref_hdr_key";
    public static final String KEY_SMILE_SHOT                = "pref_smile_shot_key";
    public static final String KEY_GESTURE_SHOT              = "pref_gesture_shot_key";
    public static final String KEY_ASD                       = "pref_asd_key";
    public static final String KEY_PHOTO_PIP                 = "photo_pip_key";
    public static final String KEY_VIDEO_PIP                 = "video_pip_key";
    public static final String KEY_LIVE_PHOTO                = "live_photo_key";
    public static final String KEY_VIDEO                     = "video_key";
    public static final String KEY_REFOCUS                   = "refocus_key";
    public static final String KEY_NORMAL                    = "normal_key";
    public static final String KEY_OBJECT_TRACKING           = "object_tracking_key";
    
    public static final String KEY_FB_EXTEME_BEAUTY_SUPPORTED    = "fb-extreme-beauty-supported";
    public static final String KEY_INTO_VIDEO_FACE_BEAUTY_NORMAL = "face-beauty-normal";
    
    // setting index
    public static final int ROW_SETTING_FACE_BEAUTY               = 0;
    public static final int ROW_SETTING_PANORAMA                  = 1;
    public static final int ROW_SETTING_MAV                       = 2;
    public static final int ROW_SETTING_MOTION_TRACK              = 3;
    public static final int ROW_SETTING_PHOTO_PIP                 = 4;
    public static final int ROW_SETTING_VIDEO_PIP                 = 5;
    public static final int ROW_SETTING_LIVE_PHOTO                = 6;
    public static final int ROW_SETTING_VIDEO                     = 7;
    public static final int ROW_SETTING_REFOCUS                   = 8;
    public static final int ROW_SETTING_NORMAL                    = 9;
    public static final int ROW_SETTING_SMILE_SHOT                = 10;//common
    public static final int ROW_SETTING_GESTURE_SHOT              = 11;
    public static final int ROW_SETTING_HDR                       = 12;//common
    public static final int ROW_SETTING_ASD                       = 13;//common
    public static final int ROW_SETTING_SLOW_MOTION               = 14;//video
    public static final int ROW_SETTING_DUAL_CAMERA               = 15;//common
    public static final int ROW_SETTING_EXPOSURE                  = 16;//common
    public static final int ROW_SETTING_SCENCE_MODE               = 17;//common
    public static final int ROW_SETTING_WHITE_BALANCE             = 18;//common
    public static final int ROW_SETTING_COLOR_EFFECT              = 19;//common
    public static final int ROW_SETTING_SELF_TIMER                = 20;//camera
    public static final int ROW_SETTING_CONTINUOUS_NUM            = 21;
    public static final int ROW_SETTING_RECORD_LOCATION           = 22;//common
    public static final int ROW_SETTING_VIDEO_QUALITY             = 23;//camera
    public static final int ROW_SETTING_ISO                       = 24;//camera
    public static final int ROW_SETTING_ANTI_FLICKER              = 25;//common
    public static final int ROW_SETTING_VIDEO_STABLE              = 26;//video
    public static final int ROW_SETTING_MICROPHONE                = 27;//video
    public static final int ROW_SETTING_AUDIO_MODE                = 28;//video
    public static final int ROW_SETTING_TIME_LAPSE                = 29;//video
    public static final int ROW_SETTING_PICTURE_RATIO             = 30;//video
    public static final int ROW_SETTING_PICTURE_SIZE              = 31;//camera
    public static final int ROW_SETTING_ZSD                       = 32;//camera
    public static final int ROW_SETTING_VOICE                     = 33;//camera
    public static final int ROW_SETTING_3DNR                      = 34;
    public static final int ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY = 35;//video
    public static final int ROW_SETTING_AIS                       = 36;//camera
    public static final int ROW_SETTING_SHARPNESS                 = 37;//common
    public static final int ROW_SETTING_HUE                       = 38;//common
    public static final int ROW_SETTING_SATURATION                = 39;//common
    public static final int ROW_SETTING_BRIGHTNESS                = 40;//common
    public static final int ROW_SETTING_CONTRAST                  = 41;//common
    public static final int ROW_SETTING_IMAGE_PROPERTIES          = 42;//common
    public static final int ROW_SETTING_CAMERA_MODE               = 43;//camera mode
    public static final int ROW_SETTING_CAPTURE_MODE              = 44;//not in preference
    public static final int ROW_SETTING_RECORDING_HINT            = 45;//not in preference
    public static final int ROW_SETTING_FLASH                     = 46;//common
    public static final int ROW_SETTING_JPEG_QUALITY              = 47;//not in preference
    public static final int ROW_SETTING_STEREO_MODE               = 48;
    public static final int ROW_SETTING_FACEBEAUTY_SMOOTH         = 49;//camera
    public static final int ROW_SETTING_FACEBEAUTY_SKIN_COLOR     = 50;//camera
    public static final int ROW_SETTING_FACEBEAUTY_SHARP          = 51;//camera
    public static final int ROW_SETTING_FACEBEAUTY_PROPERTIES     = 52;//camera
    public static final int ROW_SETTING_CAMERA_FACE_DETECT        = 53;//camera
    public static final int ROW_SETTING_MUTE_RECORDING_SOUND      = 54;
    public static final int ROW_SETTING_MULTI_FACE_MODE           = 55;
    public static final int ROW_SETTING_FACEBEAUTY_SLIM           = 56;//mean's slim effect
    public static final int ROW_SETTING_FACEBEAUTY_BIG_EYES       = 57;
    public static final int ROW_SETTING_FAST_AF                   = 58;//common
    public static final int ROW_SETTING_DISTANCE                  = 59;//common
    public static final int ROW_SETTING_DUAL_CAMERA_MODE          = 60;//common
    public static final int ROW_SETTING_OBJECT_TRACKING           = 61;
    
    public final static int NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE = 0;
    public final static int ONLY_IN_PARAMETER = 1;
    public final static int ONLY_IN_PEFERENCE = 2;
    public final static int BOTH_IN_PARAMETER_AND_PREFERENCE = 3;
    
    public static final int SUPPORTED_MAX_SOLUTION_WIDTH =  1920;
	
    private static final int[] SETTING_TYPE = new int[SETTING_COUNT];
    static {
        // setting only in preference
        SETTING_TYPE[ROW_SETTING_DUAL_CAMERA]               = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_IMAGE_PROPERTIES]          = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_RECORD_LOCATION]           = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_MICROPHONE]                = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_AUDIO_MODE]                = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_TIME_LAPSE]                = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_VIDEO_QUALITY]             = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_PICTURE_RATIO]             = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_VOICE]                     = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_STEREO_MODE]               = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_FACEBEAUTY_PROPERTIES]     = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_CAMERA_FACE_DETECT]        = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_SMILE_SHOT]                = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_ASD]                       = ONLY_IN_PEFERENCE;
        SETTING_TYPE[ROW_SETTING_GESTURE_SHOT]              = ONLY_IN_PEFERENCE;
        // setting only in parameters
        SETTING_TYPE[ROW_SETTING_CAMERA_MODE]               = ONLY_IN_PARAMETER;
        SETTING_TYPE[ROW_SETTING_CAPTURE_MODE]              = ONLY_IN_PARAMETER;
        SETTING_TYPE[ROW_SETTING_JPEG_QUALITY]              = ONLY_IN_PARAMETER;
        SETTING_TYPE[ROW_SETTING_MUTE_RECORDING_SOUND]      = ONLY_IN_PARAMETER;
        SETTING_TYPE[ROW_SETTING_FACEBEAUTY_SLIM]           = ONLY_IN_PARAMETER;
        SETTING_TYPE[ROW_SETTING_FACEBEAUTY_BIG_EYES]       = ONLY_IN_PARAMETER;
        SETTING_TYPE[ROW_SETTING_RECORDING_HINT]            = ONLY_IN_PARAMETER;
        // setting neither in parameters nor in preference
        SETTING_TYPE[ROW_SETTING_FACE_BEAUTY]               = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_PANORAMA]                  = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_MAV]                       = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_MOTION_TRACK]              = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_PHOTO_PIP]                 = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_VIDEO_PIP]                 = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_LIVE_PHOTO]                = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_VIDEO]                     = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_REFOCUS]                   = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_NORMAL]                    = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_OBJECT_TRACKING]           = NEITHER_IN_PARAMETER_NOR_IN_PREFERENCE;
        // setting both in parameters and preference
        SETTING_TYPE[ROW_SETTING_FLASH]                     = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_EXPOSURE]                  = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_SCENCE_MODE]               = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_WHITE_BALANCE]             = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_COLOR_EFFECT]              = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_SELF_TIMER]                = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_CONTINUOUS_NUM]            = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_ZSD]                       = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_PICTURE_SIZE]              = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_ISO]                       = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_ANTI_FLICKER]              = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_VIDEO_STABLE]              = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_3DNR]                      = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_SLOW_MOTION]               = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY] = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_AIS]                       = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_SHARPNESS]                 = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_HUE]                       = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_SATURATION]                = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_BRIGHTNESS]                = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_CONTRAST]                  = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_FACEBEAUTY_SMOOTH]         = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_FACEBEAUTY_SKIN_COLOR]     = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_FACEBEAUTY_SHARP]          = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_HDR]                       = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_MULTI_FACE_MODE]           = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_FAST_AF]                   = BOTH_IN_PARAMETER_AND_PREFERENCE;
        SETTING_TYPE[ROW_SETTING_DISTANCE]                  = BOTH_IN_PARAMETER_AND_PREFERENCE;
    }
    
    // setting key and index
    public static final String[] KEYS_FOR_SETTING = new String[SETTING_COUNT];
    
    static {
        KEYS_FOR_SETTING[ROW_SETTING_FLASH]                 = KEY_FLASH;
        KEYS_FOR_SETTING[ROW_SETTING_DUAL_CAMERA]           = KEY_CAMERA_ID;//need recheck
        KEYS_FOR_SETTING[ROW_SETTING_EXPOSURE]              = KEY_EXPOSURE;
        KEYS_FOR_SETTING[ROW_SETTING_SCENCE_MODE]           = KEY_SCENE_MODE;
        KEYS_FOR_SETTING[ROW_SETTING_WHITE_BALANCE]         = KEY_WHITE_BALANCE;
        KEYS_FOR_SETTING[ROW_SETTING_IMAGE_PROPERTIES]      = KEY_IMAGE_PROPERTIES;
        KEYS_FOR_SETTING[ROW_SETTING_COLOR_EFFECT]          = KEY_COLOR_EFFECT;
        KEYS_FOR_SETTING[ROW_SETTING_SELF_TIMER]            = KEY_SELF_TIMER;
        KEYS_FOR_SETTING[ROW_SETTING_ZSD]                   = KEY_CAMERA_ZSD;
        KEYS_FOR_SETTING[ROW_SETTING_RECORD_LOCATION]       = KEY_RECORD_LOCATION;//need recheck
        KEYS_FOR_SETTING[ROW_SETTING_PICTURE_SIZE]          = KEY_PICTURE_SIZE;
        KEYS_FOR_SETTING[ROW_SETTING_ISO]                   = KEY_ISO;
        KEYS_FOR_SETTING[ROW_SETTING_ANTI_FLICKER]          = KEY_ANTI_BANDING;
        KEYS_FOR_SETTING[ROW_SETTING_VIDEO_STABLE]          = KEY_VIDEO_EIS;
        KEYS_FOR_SETTING[ROW_SETTING_MICROPHONE]            = KEY_VIDEO_RECORD_AUDIO;
        KEYS_FOR_SETTING[ROW_SETTING_AUDIO_MODE]            = KEY_VIDEO_HD_AUDIO_RECORDING;
        KEYS_FOR_SETTING[ROW_SETTING_TIME_LAPSE]            = KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL;
        KEYS_FOR_SETTING[ROW_SETTING_VIDEO_QUALITY]         = KEY_VIDEO_QUALITY;
        KEYS_FOR_SETTING[ROW_SETTING_PICTURE_RATIO]         = KEY_PICTURE_RATIO;
        KEYS_FOR_SETTING[ROW_SETTING_VOICE]                 = KEY_VOICE;
        KEYS_FOR_SETTING[ROW_SETTING_3DNR]                  = KEY_VIDEO_3DNR;
        KEYS_FOR_SETTING[ROW_SETTING_SLOW_MOTION]           = KEY_SLOW_MOTION;
        KEYS_FOR_SETTING[ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY]     = KEY_SLOW_MOTION_VIDEO_QUALITY;

        KEYS_FOR_SETTING[ROW_SETTING_AIS]                   = KEY_CAMERA_AIS;

         
        KEYS_FOR_SETTING[ROW_SETTING_SHARPNESS]             = KEY_EDGE;
        KEYS_FOR_SETTING[ROW_SETTING_HUE]                   = KEY_HUE;
        KEYS_FOR_SETTING[ROW_SETTING_SATURATION]            = KEY_SATURATION;
        KEYS_FOR_SETTING[ROW_SETTING_BRIGHTNESS]            = KEY_BRIGHTNESS;
        KEYS_FOR_SETTING[ROW_SETTING_CONTRAST]              = KEY_CONTRAST;
        KEYS_FOR_SETTING[ROW_SETTING_CAMERA_MODE]           = KEY_CAMERA_MODE;
        KEYS_FOR_SETTING[ROW_SETTING_CAPTURE_MODE]          = KEY_CAPTURE_MODE;
        KEYS_FOR_SETTING[ROW_SETTING_CONTINUOUS_NUM]        = KEY_CONTINUOUS_NUMBER;
        KEYS_FOR_SETTING[ROW_SETTING_RECORDING_HINT]        = KEY_RECORDING_HIHT;
        KEYS_FOR_SETTING[ROW_SETTING_JPEG_QUALITY]          = KEY_JPEG_QUALITY;
        KEYS_FOR_SETTING[ROW_SETTING_STEREO_MODE]           = KEY_STEREO3D_MODE;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_PROPERTIES] = KEY_FACE_BEAUTY_PROPERTIES;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_SMOOTH]     = KEY_FACE_BEAUTY_SMOOTH;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_SKIN_COLOR] = KEY_FACE_BEAUTY_SKIN_COLOR;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_SHARP]      = KEY_FACE_BEAUTY_SHARP;
        KEYS_FOR_SETTING[ROW_SETTING_CAMERA_FACE_DETECT]    = KEY_CAMERA_FACE_DETECT;
        KEYS_FOR_SETTING[ROW_SETTING_HDR]                   = KEY_HDR;
        KEYS_FOR_SETTING[ROW_SETTING_SMILE_SHOT]            = KEY_SMILE_SHOT;
        KEYS_FOR_SETTING[ROW_SETTING_ASD]                   = KEY_ASD;
        KEYS_FOR_SETTING[ROW_SETTING_MUTE_RECORDING_SOUND]  = KEY_MUTE_RECORDING_SOUND;
        KEYS_FOR_SETTING[ROW_SETTING_GESTURE_SHOT]          = KEY_GESTURE_SHOT;
        KEYS_FOR_SETTING[ROW_SETTING_MULTI_FACE_MODE]       = KEY_MULTI_FACE_BEAUTY;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_SLIM]       = KEY_FACE_BEAUTY_SLIM;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_BIG_EYES]   = KEY_FACE_BEAUTY_BIG_EYES;
        KEYS_FOR_SETTING[ROW_SETTING_DUAL_CAMERA_MODE]      = KEY_DUAL_CAMERA_MODE;
        KEYS_FOR_SETTING[ROW_SETTING_FAST_AF]               = KEY_FAST_AF;
        KEYS_FOR_SETTING[ROW_SETTING_DISTANCE]              = KEY_DISTANCE;
        KEYS_FOR_SETTING[ROW_SETTING_FACE_BEAUTY]           = KEY_FACE_BEAUTY;
        KEYS_FOR_SETTING[ROW_SETTING_PANORAMA]              = KEY_PANORAMA;
        KEYS_FOR_SETTING[ROW_SETTING_MAV]                   = KEY_MAV;
        KEYS_FOR_SETTING[ROW_SETTING_MOTION_TRACK]          = KEY_MOTION_TRACK;
        KEYS_FOR_SETTING[ROW_SETTING_PHOTO_PIP]             = KEY_PHOTO_PIP;
        KEYS_FOR_SETTING[ROW_SETTING_VIDEO_PIP]             = KEY_VIDEO_PIP;
        KEYS_FOR_SETTING[ROW_SETTING_LIVE_PHOTO]            = KEY_LIVE_PHOTO;
        KEYS_FOR_SETTING[ROW_SETTING_VIDEO]                 = KEY_VIDEO;
        KEYS_FOR_SETTING[ROW_SETTING_REFOCUS]               = KEY_REFOCUS;
        KEYS_FOR_SETTING[ROW_SETTING_NORMAL]                = KEY_NORMAL;
        KEYS_FOR_SETTING[ROW_SETTING_OBJECT_TRACKING]       = KEY_OBJECT_TRACKING;
    };
    
    public static final int[] SETTING_GROUP_COMMON_FOR_TAB = new int[]{
        ROW_SETTING_DUAL_CAMERA_MODE,
        ROW_SETTING_RECORD_LOCATION,//common
        ROW_SETTING_MULTI_FACE_MODE,
        ROW_SETTING_EXPOSURE,//common
        ROW_SETTING_COLOR_EFFECT,//common
        ROW_SETTING_SCENCE_MODE,//common
        ROW_SETTING_WHITE_BALANCE,//common
        ROW_SETTING_IMAGE_PROPERTIES,
        ROW_SETTING_ANTI_FLICKER,//common
    };
    
    public static final int[] SETTING_GROUP_MAIN_COMMON_FOR_TAB = new int[]{
        ROW_SETTING_RECORD_LOCATION,//common
        ROW_SETTING_MULTI_FACE_MODE,
        ROW_SETTING_IMAGE_PROPERTIES,
        ROW_SETTING_ANTI_FLICKER,//common
    };
    
    public static final int[] SETTING_GROUP_COMMON_FOR_LOMOEFFECT = new int[]{
        ROW_SETTING_DUAL_CAMERA_MODE,
        ROW_SETTING_RECORD_LOCATION,//common
        ROW_SETTING_MULTI_FACE_MODE,
        ROW_SETTING_EXPOSURE,//common
        ROW_SETTING_SCENCE_MODE,//common
        ROW_SETTING_WHITE_BALANCE,//common
        ROW_SETTING_IMAGE_PROPERTIES,
        ROW_SETTING_ANTI_FLICKER,//common
    };
    
    public static final int[] SETTING_GROUP_CAMERA_FOR_TAB = new int[]{
        ROW_SETTING_ZSD,//camera
        ROW_SETTING_AIS,//camera
        ROW_SETTING_VOICE,//camera
        ROW_SETTING_CAMERA_FACE_DETECT,//camera
        ROW_SETTING_GESTURE_SHOT,
        ROW_SETTING_SMILE_SHOT,//camera
        ROW_SETTING_ASD,//camera
        ROW_SETTING_SELF_TIMER,//camera
        ROW_SETTING_CONTINUOUS_NUM,//camera
        ROW_SETTING_PICTURE_SIZE,//camera
        ROW_SETTING_PICTURE_RATIO,//camera
        ROW_SETTING_ISO,//camera
        ROW_SETTING_FACEBEAUTY_PROPERTIES,//camera:Cfb
    };
    
    public static final int[] SETTING_GROUP_VIDEO_FOR_TAB = new int[]{
        ROW_SETTING_3DNR,
        ROW_SETTING_VIDEO_STABLE,//video
        ROW_SETTING_MICROPHONE,//video
        ROW_SETTING_AUDIO_MODE,//video
        ROW_SETTING_TIME_LAPSE,//video
        ROW_SETTING_VIDEO_QUALITY,//video
        ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY,
    };
    
    // Preview setting items for final user(brief case), order by UX spec
    public static final int[] SETTING_GROUP_COMMON_FOR_TAB_PREVIEW = new int[] {
        ROW_SETTING_PICTURE_SIZE,// camera
        ROW_SETTING_PICTURE_RATIO,// camera
        ROW_SETTING_VIDEO_QUALITY,// video
        ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY, 
     };
    
    // Camera setting items for final user, order by UX spec
    public static final int[] SETTING_GROUP_CAMERA_FOR_TAB_NO_PREVIEW = new int[] {
        ROW_SETTING_AIS,// camera
        ROW_SETTING_ZSD,// camera
        ROW_SETTING_VOICE,// camera
        ROW_SETTING_CAMERA_FACE_DETECT,// camera
        ROW_SETTING_SELF_TIMER,// camera
        ROW_SETTING_CONTINUOUS_NUM,// camera
        ROW_SETTING_ISO,// camera
    };
    
    // Video setting items for final user, order by UX spec
    public static final int[] SETTING_GROUP_VIDEO_FOR_TAB_NO_PREVIEW = new int[] {
        ROW_SETTING_3DNR, 
        ROW_SETTING_VIDEO_STABLE,// video
        ROW_SETTING_MICROPHONE,// video
        ROW_SETTING_AUDIO_MODE,// video
        ROW_SETTING_TIME_LAPSE,// video
    };
    
    // Camera setting items for 3d
    public static final int[] SETTING_GROUP_CAMERA_3D_FOR_TAB = new int[] {
        ROW_SETTING_ZSD,// camera
        ROW_SETTING_VOICE,// camera
        ROW_SETTING_CAMERA_FACE_DETECT,// camera
        ROW_SETTING_SELF_TIMER,// camera
        ROW_SETTING_CONTINUOUS_NUM,// camera
        // ROW_SETTING_PICTURE_SIZE,// picture size is not allowed in 3d
        ROW_SETTING_PICTURE_RATIO,// camera
        ROW_SETTING_ISO,// camera
        ROW_SETTING_FACEBEAUTY_PROPERTIES,// camera
    };
    
    public static final int[] UN_SUPPORT_BY_3RDPARTY = new int[] {
        ROW_SETTING_CONTINUOUS_NUM,
        ROW_SETTING_HDR,
        ROW_SETTING_SMILE_SHOT,
        ROW_SETTING_ASD,
        ROW_SETTING_GESTURE_SHOT,
        ROW_SETTING_SLOW_MOTION,
        ROW_SETTING_MULTI_FACE_MODE,
        ROW_SETTING_FACEBEAUTY_PROPERTIES,
    };
    
    public static final int[] UN_SUPPORT_BY_FRONT_CAMERA = new int[] {
        ROW_SETTING_SLOW_MOTION,
        ROW_SETTING_CONTINUOUS_NUM,
        ROW_SETTING_SLOW_MOTION_VIDEO_QUALITY,
        ROW_SETTING_STEREO_MODE,
    };
    
    // Camera all setting items for final user.
    public static final int[] SETTING_GROUP_CAMERA_FOR_UI = new int[] {
        ROW_SETTING_FLASH,// common
        ROW_SETTING_EXPOSURE,// common
        ROW_SETTING_SCENCE_MODE,// common
        ROW_SETTING_WHITE_BALANCE,// common
        ROW_SETTING_COLOR_EFFECT,// common
        ROW_SETTING_RECORD_LOCATION,// common
        ROW_SETTING_ANTI_FLICKER,// common
        ROW_SETTING_3DNR,
        ROW_SETTING_SELF_TIMER,// camera
        ROW_SETTING_AIS,
        ROW_SETTING_ZSD,// camera
        ROW_SETTING_CONTINUOUS_NUM,// camera
        ROW_SETTING_PICTURE_SIZE,// camera
        ROW_SETTING_ISO,// camera
        ROW_SETTING_FACEBEAUTY_PROPERTIES,// camera
        ROW_SETTING_FACEBEAUTY_SMOOTH, 
        ROW_SETTING_FACEBEAUTY_SKIN_COLOR,
        ROW_SETTING_FACEBEAUTY_SHARP,
            
        ROW_SETTING_SHARPNESS,// common
        ROW_SETTING_HUE,// common
        ROW_SETTING_SATURATION,// common
        ROW_SETTING_BRIGHTNESS,// common
        ROW_SETTING_CONTRAST,// common
            
        ROW_SETTING_PICTURE_RATIO,// camera
        ROW_SETTING_VOICE,// camera
        ROW_SETTING_CAMERA_FACE_DETECT,// camera
        ROW_SETTING_MULTI_FACE_MODE,// vFB Camera
        ROW_SETTING_HDR, 
        ROW_SETTING_SMILE_SHOT, 
        ROW_SETTING_ASD, };
    
    // Video all setting items for final user.
    public static final int[] SETTING_GROUP_VIDEO_FOR_UI = new int[] {
        ROW_SETTING_FLASH,// common
        ROW_SETTING_EXPOSURE,// common
        ROW_SETTING_SCENCE_MODE,// common moved to head
        ROW_SETTING_WHITE_BALANCE,// common
        ROW_SETTING_COLOR_EFFECT,// common
        ROW_SETTING_RECORD_LOCATION,// common
        ROW_SETTING_ANTI_FLICKER,// common
        ROW_SETTING_3DNR,// video
        ROW_SETTING_VIDEO_STABLE,// video
        ROW_SETTING_MICROPHONE,// video
        ROW_SETTING_AUDIO_MODE,// video
        ROW_SETTING_TIME_LAPSE,// video
            
        ROW_SETTING_SHARPNESS,// common
        ROW_SETTING_HUE,// common
        ROW_SETTING_SATURATION,// common
        ROW_SETTING_BRIGHTNESS,// common
        ROW_SETTING_CONTRAST,// common
    };
    
    // For Tablet feature.
    public static final int[] SETTING_GROUP_SUB_COMMON = new int[] {
        ROW_SETTING_EXPOSURE,// common
        ROW_SETTING_COLOR_EFFECT,// common
        ROW_SETTING_WHITE_BALANCE,// common
        ROW_SETTING_SCENCE_MODE,// common
    };
    
    public static final int[] RESET_SETTING_ITEMS = new int[] {
        ROW_SETTING_EXPOSURE,
        ROW_SETTING_SCENCE_MODE,
        ROW_SETTING_WHITE_BALANCE,
        ROW_SETTING_COLOR_EFFECT,
        ROW_SETTING_SELF_TIMER,
        ROW_SETTING_SHARPNESS,// common
        ROW_SETTING_HUE,// common
        ROW_SETTING_SATURATION,// common
        ROW_SETTING_BRIGHTNESS,// common
        ROW_SETTING_CONTRAST,// common
        ROW_SETTING_ISO,
        ROW_SETTING_TIME_LAPSE,
        ROW_SETTING_AUDIO_MODE, 
        ROW_SETTING_HDR, 
        ROW_SETTING_3DNR,
        ROW_SETTING_SMILE_SHOT, 
        ROW_SETTING_ASD, 
        ROW_SETTING_SLOW_MOTION,
        ROW_SETTING_GESTURE_SHOT, 
    };
    
    public static final int[] THIRDPART_RESET_SETTING_ITEMS = new int[] {
        ROW_SETTING_EXPOSURE, 
        ROW_SETTING_SCENCE_MODE, 
        ROW_SETTING_WHITE_BALANCE,
        ROW_SETTING_COLOR_EFFECT,
        ROW_SETTING_SELF_TIMER, 
        ROW_SETTING_SHARPNESS,// common
        ROW_SETTING_HUE,// common
        ROW_SETTING_SATURATION,// common
        ROW_SETTING_BRIGHTNESS,// common
        ROW_SETTING_CONTRAST,// common
        ROW_SETTING_ISO,
        ROW_SETTING_3DNR,
        ROW_SETTING_TIME_LAPSE, 
        ROW_SETTING_AUDIO_MODE, 
   };
    
    public static String getSettingKey(int settingId) {
        return KEYS_FOR_SETTING[settingId];
    }

    public static int getSettingId(String key) {
        int settingIndex = -1;
        for (int i = 0; i < KEYS_FOR_SETTING.length; i++) {
            if (KEYS_FOR_SETTING[i].equals(key)) {
                settingIndex = i;
                break;
            }
        }
        return settingIndex;
    }
    
    public static int getSettingType(int settingId) {
        return SETTING_TYPE[settingId];
    }
}
