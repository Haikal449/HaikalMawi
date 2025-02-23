/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
* Copyright (C) 2013 Samsung System LSI
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.bluetooth.map;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import android.util.Log;

/**
 * This class encapsulates the appParams needed for MAP.
 */
public class BluetoothMapAppParams {

    private static final String TAG = "[MAP]BluetoothMapAppParams";

    private static final int MAX_LIST_COUNT           = 0x01;
    private static final int MAX_LIST_COUNT_LEN       = 0x02; //, 0x0000, 0xFFFF),
    private static final int START_OFFSET             = 0x02;
    private static final int START_OFFSET_LEN         = 0x02; //, 0x0000, 0xFFFF),
    private static final int FILTER_MESSAGE_TYPE      = 0x03;
    private static final int FILTER_MESSAGE_TYPE_LEN  = 0x01; //, 0x0000, 0x000f),
    private static final int FILTER_PERIOD_BEGIN      = 0x04;
    private static final int FILTER_PERIOD_END        = 0x05;
    private static final int FILTER_READ_STATUS       = 0x06;
    private static final int FILTER_READ_STATUS_LEN   = 0x01; //, 0x0000, 0x0002),
    private static final int FILTER_RECIPIENT         = 0x07;
    private static final int FILTER_ORIGINATOR        = 0x08;
    private static final int FILTER_PRIORITY          = 0x09;
    private static final int FILTER_PRIORITY_LEN      = 0x01; //, 0x0000, 0x0002),
    private static final int ATTACHMENT               = 0x0A;
    private static final int ATTACHMENT_LEN           = 0x01; //, 0x0000, 0x0001),
    private static final int TRANSPARENT              = 0x0B;
    private static final int TRANSPARENT_LEN          = 0x01; //, 0x0000, 0x0001),
    private static final int RETRY                    = 0x0C;
    private static final int RETRY_LEN                = 0x01; //, 0x0000, 0x0001),
    private static final int NEW_MESSAGE              = 0x0D;
    private static final int NEW_MESSAGE_LEN          = 0x01; //, 0x0000, 0x0001),
    private static final int NOTIFICATION_STATUS      = 0x0E;
    private static final int NOTIFICATION_STATUS_LEN  = 0x01; //, 0x0000, 0xFFFF),
    private static final int MAS_INSTANCE_ID          = 0x0F;
    private static final int MAS_INSTANCE_ID_LEN      = 0x01; //, 0x0000, 0x00FF),
    private static final int PARAMETER_MASK           = 0x10;
    private static final int PARAMETER_MASK_LEN       = 0x04; //, 0x0000, 0x0000),
    private static final int FOLDER_LISTING_SIZE      = 0x11;
    private static final int FOLDER_LISTING_SIZE_LEN  = 0x02; //, 0x0000, 0xFFFF),
    private static final int MESSAGE_LISTING_SIZE     = 0x12;
    private static final int MESSAGE_LISTING_SIZE_LEN = 0x02; //, 0x0000, 0xFFFF),
    private static final int SUBJECT_LENGTH           = 0x13;
    private static final int SUBJECT_LENGTH_LEN       = 0x01; //, 0x0000, 0x00FF),
    private static final int CHARSET                  = 0x14;
    private static final int CHARSET_LEN              = 0x01; //, 0x0000, 0x0001),
    private static final int FRACTION_REQUEST         = 0x15;
    private static final int FRACTION_REQUEST_LEN     = 0x01; //, 0x0000, 0x0001),
    private static final int FRACTION_DELIVER         = 0x16;
    private static final int FRACTION_DELIVER_LEN     = 0x01; //, 0x0000, 0x0001),
    private static final int STATUS_INDICATOR         = 0x17;
    private static final int STATUS_INDICATOR_LEN     = 0x01; //, 0x0000, 0x0001),
    private static final int STATUS_VALUE             = 0x18;
    private static final int STATUS_VALUE_LEN         = 0x01; //, 0x0000, 0x0001),
    private static final int MSE_TIME                 = 0x19;

    public static final int INVALID_VALUE_PARAMETER = -1;
    public static final int NOTIFICATION_STATUS_NO = 0;
    public static final int NOTIFICATION_STATUS_YES = 1;
    public static final int STATUS_INDICATOR_READ = 0;
    public static final int STATUS_INDICATOR_DELETED = 1;
    public static final int STATUS_VALUE_YES = 1;
    public static final int STATUS_VALUE_NO = 0;
    public static final int CHARSET_NATIVE = 0;
    public static final int CHARSET_UTF8 = 1;
    public static final int FRACTION_REQUEST_FIRST = 0;
    public static final int FRACTION_REQUEST_NEXT = 1;
    public static final int FRACTION_DELIVER_MORE = 0;
    public static final int FRACTION_DELIVER_LAST = 1;


    public static final int FILTER_NO_SMS_GSM  = 0x01;
    public static final int FILTER_NO_SMS_CDMA = 0x02;
    public static final int FILTER_NO_EMAIL    = 0x04;
    public static final int FILTER_NO_MMS      = 0x08;

    /* Default values for omitted application parameters */
    public static final long PARAMETER_MASK_ALL_ENABLED = 0xFFFF; // TODO: Update when bit 16-31 will be used.

    private int mMaxListCount        = INVALID_VALUE_PARAMETER;
    private int mStartOffset         = INVALID_VALUE_PARAMETER;
    private int mFilterMessageType   = INVALID_VALUE_PARAMETER;
    private long mFilterPeriodBegin  = INVALID_VALUE_PARAMETER;
    private long mFilterPeriodEnd    = INVALID_VALUE_PARAMETER;
    private int mFilterReadStatus    = INVALID_VALUE_PARAMETER;
    private String mFilterRecipient   = null;
    private String mFilterOriginator  = null;
    private int mFilterPriority      = INVALID_VALUE_PARAMETER;
    private int mAttachment          = INVALID_VALUE_PARAMETER;
    private int mTransparent         = INVALID_VALUE_PARAMETER;
    private int mRetry               = INVALID_VALUE_PARAMETER;
    private int mNewMessage          = INVALID_VALUE_PARAMETER;
    private int mNotificationStatus  = INVALID_VALUE_PARAMETER;
    private int mMasInstanceId       = INVALID_VALUE_PARAMETER;
    private long mParameterMask      = INVALID_VALUE_PARAMETER;
    private int mFolderListingSize   = INVALID_VALUE_PARAMETER;
    private int mMessageListingSize  = INVALID_VALUE_PARAMETER;
    private int mSubjectLength       = INVALID_VALUE_PARAMETER;
    private int mCharset             = INVALID_VALUE_PARAMETER;
    private int mFractionRequest     = INVALID_VALUE_PARAMETER;
    private int mFractionDeliver     = INVALID_VALUE_PARAMETER;
    private int mStatusIndicator     = INVALID_VALUE_PARAMETER;
    private int mStatusValue         = INVALID_VALUE_PARAMETER;
    private long mMseTime            = INVALID_VALUE_PARAMETER;

    /**
     * Default constructor, used to build an application parameter object to be
     * encoded. By default the member variables will be initialized to
     * {@link INVALID_VALUE_PARAMETER} for values, and empty strings for String
     * typed members.
     */
    public BluetoothMapAppParams() {
    }

    /**
     * Creates an application parameter object based on a application parameter
     * OBEX header. The content of the {@link appParam} byte array will be
     * parsed, and its content will be stored in the member variables.
     * {@link INVALID_VALUE_PARAMETER} can be used to determine if a value is
     * set or not, where strings will be empty, if {@link appParam} did not
     * contain the parameter.
     *
     * @param appParams
     *            the byte array containing the application parameters OBEX
     *            header
     * @throws IllegalArgumentException
     *             when a parameter does not respect the valid ranges specified
     *             in the MAP spec.
     * @throws ParseException
     *             if a parameter string if formated incorrectly.
     */
    public BluetoothMapAppParams(final byte[] appParams)
                 throws IllegalArgumentException, ParseException {
        ParseParams(appParams);
    }

    /**
     * Parse an application parameter OBEX header stored in a ByteArray.
     *
     * @param appParams
     *            the byte array containing the application parameters OBEX
     *            header
     * @throws IllegalArgumentException
     *             when a parameter does not respect the valid ranges specified
     *             in the MAP spec.
     * @throws ParseException
     *             if a parameter string if formated incorrectly.
     */
    private void ParseParams(final byte[] appParams) throws ParseException,
              IllegalArgumentException {
        int i = 0;
        int tagId, tagLength;
        ByteBuffer appParamBuf = ByteBuffer.wrap(appParams);
        appParamBuf.order(ByteOrder.BIG_ENDIAN);
        while (i < appParams.length) {
            tagId = appParams[i++] & 0xff;     // Convert to unsigned to support values above 127
            tagLength = appParams[i++] & 0xff; // Convert to unsigned to support values above 127
            switch (tagId) {
            case MAX_LIST_COUNT:
                if (tagLength != MAX_LIST_COUNT_LEN) {
                    Log.w(TAG, "[ParseParams] MAX_LIST_COUNT: Wrong length received: "
                            + tagLength + " expected: " + MAX_LIST_COUNT_LEN);
                    break;
                }
                setMaxListCount(appParamBuf.getShort(i) & 0xffff); // Make it unsigned
                break;
            case START_OFFSET:
                if (tagLength != START_OFFSET_LEN) {
                    Log.w(TAG, "[ParseParams] START_OFFSET: Wrong length received: "
                            + tagLength + " expected: " + START_OFFSET_LEN);
                    break;
                }
                setStartOffset(appParamBuf.getShort(i) & 0xffff); // Make it unsigned
                break;
            case FILTER_MESSAGE_TYPE:
                if (tagLength != FILTER_MESSAGE_TYPE_LEN) {
                    Log.w(TAG, "[ParseParams] FILTER_MESSAGE_TYPE: Wrong length received: "
                            + tagLength + " expected: " + FILTER_MESSAGE_TYPE_LEN);
                    break;
                }
                    setFilterMessageType(appParams[i] & 0x0f);
                break;
            case FILTER_PERIOD_BEGIN:
                if(tagLength != 0) {
                    setFilterPeriodBegin(new String(appParams, i, tagLength));
                }
                break;
            case FILTER_PERIOD_END:
                if(tagLength != 0) {
                    setFilterPeriodEnd(new String(appParams, i, tagLength));
                }
                break;
            case FILTER_READ_STATUS:
                if (tagLength != FILTER_READ_STATUS_LEN) {
                     Log.w(TAG, "[ParseParams] FILTER_READ_STATUS: Wrong length received: "
                             + tagLength + " expected: " + FILTER_READ_STATUS_LEN);
                     break;
                 }
                setFilterReadStatus(appParams[i] & 0x03); // Lower two bits
                break;
            case FILTER_RECIPIENT:
                if(tagLength != 0) {
                    setFilterRecipient(new String(appParams, i, tagLength));
                }
                break;
            case FILTER_ORIGINATOR:
                if(tagLength != 0) {
                    setFilterOriginator(new String(appParams, i, tagLength));
                }
                break;
            case FILTER_PRIORITY:
                if (tagLength != FILTER_PRIORITY_LEN) {
                     Log.w(TAG, "[ParseParams] FILTER_PRIORITY: Wrong length received: "
                             + tagLength + " expected: " + FILTER_PRIORITY_LEN);
                     break;
                }
                setFilterPriority(appParams[i] & 0x03); // Lower two bits
                break;
            case ATTACHMENT:
                if (tagLength != ATTACHMENT_LEN) {
                     Log.w(TAG, "[ParseParams] ATTACHMENT: Wrong length received: "
                             + tagLength + " expected: " + ATTACHMENT_LEN);
                     break;
                }
                setAttachment(appParams[i] & 0x01); // Lower bit
                break;
            case TRANSPARENT:
                if (tagLength != TRANSPARENT_LEN) {
                     Log.w(TAG, "[ParseParams] TRANSPARENT: Wrong length received: "
                             + tagLength + " expected: " + TRANSPARENT_LEN);
                     break;
                }
                setTransparent(appParams[i] & 0x01); // Lower bit
                break;
            case RETRY:
                if (tagLength != RETRY_LEN) {
                     Log.w(TAG, "[ParseParams] RETRY: Wrong length received: "
                             + tagLength + " expected: " + RETRY_LEN);
                     break;
                }
                setRetry(appParams[i] & 0x01); // Lower bit
                break;
            case NEW_MESSAGE:
                if (tagLength != NEW_MESSAGE_LEN) {
                     Log.w(TAG, "[ParseParams] NEW_MESSAGE: Wrong length received: "
                             + tagLength + " expected: " + NEW_MESSAGE_LEN);
                     break;
                }
                setNewMessage(appParams[i] & 0x01); // Lower bit
                break;
            case NOTIFICATION_STATUS:
                if (tagLength != NOTIFICATION_STATUS_LEN) {
                     Log.w(TAG, "[ParseParams] NOTIFICATION_STATUS: Wrong length received: "
                             + tagLength + " expected: " + NOTIFICATION_STATUS_LEN);
                     break;
                }
                setNotificationStatus(appParams[i] & 0x01); // Lower bit
                break;
            case MAS_INSTANCE_ID:
                if (tagLength != MAS_INSTANCE_ID_LEN) {
                    Log.w(TAG, "[ParseParams] MAS_INSTANCE_ID: Wrong length received: "
                            + tagLength + " expected: " + MAS_INSTANCE_ID_LEN);
                    break;
                }
                setMasInstanceId(appParams[i] & 0xff);
                break;
            case PARAMETER_MASK:
                if (tagLength != PARAMETER_MASK_LEN) {
                    Log.w(TAG, "[ParseParams] PARAMETER_MASK: Wrong length received: "
                            + tagLength + " expected: " + PARAMETER_MASK_LEN);
                    break;
                }
                setParameterMask(appParamBuf.getInt(i) & 0xffffffffL); // Make it unsigned
                break;
            case FOLDER_LISTING_SIZE:
                if (tagLength != FOLDER_LISTING_SIZE_LEN) {
                    Log.w(TAG, "[ParseParams] FOLDER_LISTING_SIZE: Wrong length received: "
                            + tagLength + " expected: " + FOLDER_LISTING_SIZE_LEN);
                    break;
                }
                setFolderListingSize(appParamBuf.getShort(i) & 0xffff); // Make it unsigned
                break;
            case MESSAGE_LISTING_SIZE:
                if (tagLength != MESSAGE_LISTING_SIZE_LEN) {
                    Log.w(TAG, "[ParseParams] MESSAGE_LISTING_SIZE: Wrong length received: "
                            + tagLength + " expected: " + MESSAGE_LISTING_SIZE_LEN);
                    break;
                }
                setMessageListingSize(appParamBuf.getShort(i) & 0xffff); // Make it unsigned
                break;
            case SUBJECT_LENGTH:
                if (tagLength != SUBJECT_LENGTH_LEN) {
                    Log.w(TAG, "[ParseParams] SUBJECT_LENGTH: Wrong length received: "
                            + tagLength + " expected: " + SUBJECT_LENGTH_LEN);
                    break;
                }
                setSubjectLength(appParams[i] & 0xff);
                break;
            case CHARSET:
                if (tagLength != CHARSET_LEN) {
                    Log.w(TAG, "[ParseParams] CHARSET: Wrong length received: "
                            + tagLength + " expected: " + CHARSET_LEN);
                    break;
                }
                setCharset(appParams[i] & 0x01); // Lower bit
                break;
            case FRACTION_REQUEST:
                if (tagLength != FRACTION_REQUEST_LEN) {
                    Log.w(TAG, "[ParseParams] FRACTION_REQUEST: Wrong length received: "
                            + tagLength + " expected: " + FRACTION_REQUEST_LEN);
                    break;
                }
                setFractionRequest(appParams[i] & 0x01); // Lower bit
                break;
            case FRACTION_DELIVER:
                if (tagLength != FRACTION_DELIVER_LEN) {
                    Log.w(TAG, "[ParseParams] FRACTION_DELIVER: Wrong length received: "
                            + tagLength + " expected: " + FRACTION_DELIVER_LEN);
                    break;
                }
                setFractionDeliver(appParams[i] & 0x01); // Lower bit
                break;
            case STATUS_INDICATOR:
                if (tagLength != STATUS_INDICATOR_LEN) {
                    Log.w(TAG, "[ParseParams] STATUS_INDICATOR: Wrong length received: "
                            + tagLength + " expected: " + STATUS_INDICATOR_LEN);
                    break;
                }
                setStatusIndicator(appParams[i] & 0x01); // Lower bit
                break;
            case STATUS_VALUE:
                if (tagLength != STATUS_VALUE_LEN) {
                    Log.w(TAG, "[ParseParams] STATUS_VALUER: Wrong length received: "
                            + tagLength + " expected: " + STATUS_VALUE_LEN);
                    break;
                }
                setStatusValue(appParams[i] & 0x01); // Lower bit
                break;
            case MSE_TIME:
                setMseTime(new String(appParams, i, tagLength));
                break;
            default:
                // Just skip unknown Tags, no need to report error
                Log.w(TAG, "[ParseParams] Unknown TagId received ( 0x"
                        + Integer.toString(tagId, 16) + "), skipping...");
                break;
            }
            i += tagLength; // Offset to next TagId
        }
    }

    /**
     * Get the approximate length needed to store the appParameters in a byte
     * array.
     *
     * @return the length in bytes
     * @throws UnsupportedEncodingException
     *             if the platform does not support UTF-8 encoding.
     */
    private int getParamMaxLength() throws UnsupportedEncodingException {
        int length = 0;
        length += 25 * 2; // tagId + tagLength
        length += 27; // fixed sizes
        length += getFilterPeriodBegin() == INVALID_VALUE_PARAMETER ? 0 : 15;
        length += getFilterPeriodEnd() == INVALID_VALUE_PARAMETER ? 0 : 15;
        if (getFilterRecipient() != null)
            length += getFilterRecipient().getBytes("UTF-8").length;
        if (getFilterOriginator() != null)
            length += getFilterOriginator().getBytes("UTF-8").length;
        length += getMseTime() == INVALID_VALUE_PARAMETER ? 0 : 20;
        return length;
    }

    /**
     * Encode the application parameter object to a byte array.
     *
     * @return a byte Array representation of the application parameter object.
     * @throws UnsupportedEncodingException
     *             if the platform does not support UTF-8 encoding.
     */
    public byte[] EncodeParams() throws UnsupportedEncodingException {
        ByteBuffer appParamBuf = ByteBuffer.allocate(getParamMaxLength());
        appParamBuf.order(ByteOrder.BIG_ENDIAN);
        byte[] retBuf;

        if (getMaxListCount() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) MAX_LIST_COUNT);
            appParamBuf.put((byte) MAX_LIST_COUNT_LEN);
            appParamBuf.putShort((short) getMaxListCount());
        }
        if (getStartOffset() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) START_OFFSET);
            appParamBuf.put((byte) START_OFFSET_LEN);
            appParamBuf.putShort((short) getStartOffset());
        }
        if (getFilterMessageType() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) FILTER_MESSAGE_TYPE);
            appParamBuf.put((byte) FILTER_MESSAGE_TYPE_LEN);
            appParamBuf.put((byte) getFilterMessageType());
        }
        if (getFilterPeriodBegin() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) FILTER_PERIOD_BEGIN);
            appParamBuf.put((byte) getFilterPeriodBeginString().getBytes("UTF-8").length);
            appParamBuf.put(getFilterPeriodBeginString().getBytes("UTF-8"));
        }
        if (getFilterPeriodEnd() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) FILTER_PERIOD_END);
            appParamBuf.put((byte) getFilterPeriodEndString().getBytes("UTF-8").length);
            appParamBuf.put(getFilterPeriodEndString().getBytes("UTF-8"));
        }
        if (getFilterReadStatus() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) FILTER_READ_STATUS);
            appParamBuf.put((byte) FILTER_READ_STATUS_LEN);
            appParamBuf.put((byte) getFilterReadStatus());
        }
        if (getFilterRecipient() != null) {
            appParamBuf.put((byte) FILTER_RECIPIENT);
            appParamBuf.put((byte) getFilterRecipient().getBytes("UTF-8").length);
            appParamBuf.put(getFilterRecipient().getBytes("UTF-8"));
        }
        if (getFilterOriginator() != null) {
            appParamBuf.put((byte) FILTER_ORIGINATOR);
            appParamBuf.put((byte) getFilterOriginator().getBytes("UTF-8").length);
            appParamBuf.put(getFilterOriginator().getBytes("UTF-8"));
        }
        if (getFilterPriority() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) FILTER_PRIORITY);
            appParamBuf.put((byte) FILTER_PRIORITY_LEN);
            appParamBuf.put((byte) getFilterPriority());
        }
        if (getAttachment() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) ATTACHMENT);
            appParamBuf.put((byte) ATTACHMENT_LEN);
            appParamBuf.put((byte) getAttachment());
        }
        if (getTransparent() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) TRANSPARENT);
            appParamBuf.put((byte) TRANSPARENT_LEN);
            appParamBuf.put((byte) getTransparent());
        }
        if (getRetry() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) RETRY);
            appParamBuf.put((byte) RETRY_LEN);
            appParamBuf.put((byte) getRetry());
        }
        if (getNewMessage() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) NEW_MESSAGE);
            appParamBuf.put((byte) NEW_MESSAGE_LEN);
            appParamBuf.put((byte) getNewMessage());
        }
        if (getNotificationStatus() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) NOTIFICATION_STATUS);
            appParamBuf.put((byte) NOTIFICATION_STATUS_LEN);
            appParamBuf.putShort((short) getNotificationStatus());
        }
        if (getMasInstanceId() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) MAS_INSTANCE_ID);
            appParamBuf.put((byte) MAS_INSTANCE_ID_LEN);
            appParamBuf.put((byte) getMasInstanceId());
        }
        if (getParameterMask() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) PARAMETER_MASK);
            appParamBuf.put((byte) PARAMETER_MASK_LEN);
            appParamBuf.putInt((int) getParameterMask());
        }
        if (getFolderListingSize() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) FOLDER_LISTING_SIZE);
            appParamBuf.put((byte) FOLDER_LISTING_SIZE_LEN);
            appParamBuf.putShort((short) getFolderListingSize());
        }
        if (getMessageListingSize() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) MESSAGE_LISTING_SIZE);
            appParamBuf.put((byte) MESSAGE_LISTING_SIZE_LEN);
            appParamBuf.putShort((short) getMessageListingSize());
        }
        if (getSubjectLength() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) SUBJECT_LENGTH);
            appParamBuf.put((byte) SUBJECT_LENGTH_LEN);
            appParamBuf.put((byte) getSubjectLength());
        }
        if (getCharset() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) CHARSET);
            appParamBuf.put((byte) CHARSET_LEN);
            appParamBuf.put((byte) getCharset());
        }
        if (getFractionRequest() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) FRACTION_REQUEST);
            appParamBuf.put((byte) FRACTION_REQUEST_LEN);
            appParamBuf.put((byte) getFractionRequest());
        }
        if (getFractionDeliver() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) FRACTION_DELIVER);
            appParamBuf.put((byte) FRACTION_DELIVER_LEN);
            appParamBuf.put((byte) getFractionDeliver());
        }
        if (getStatusIndicator() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) STATUS_INDICATOR);
            appParamBuf.put((byte) STATUS_INDICATOR_LEN);
            appParamBuf.put((byte) getStatusIndicator());
        }
        if (getStatusValue() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) STATUS_VALUE);
            appParamBuf.put((byte) STATUS_VALUE_LEN);
            appParamBuf.put((byte) getStatusValue());
        }
        if (getMseTime() != INVALID_VALUE_PARAMETER) {
            appParamBuf.put((byte) MSE_TIME);
            appParamBuf.put((byte) getMseTimeString().getBytes("UTF-8").length);
            appParamBuf.put(getMseTimeString().getBytes("UTF-8"));
        }
        // We need to reduce the length of the array to match the content
        retBuf = Arrays.copyOfRange(appParamBuf.array(), appParamBuf.arrayOffset(),
                                    appParamBuf.arrayOffset() + appParamBuf.position());
        return retBuf;
    }

    public int getMaxListCount() {
        return mMaxListCount;
    }

    public void setMaxListCount(int maxListCount) throws IllegalArgumentException {
        if (maxListCount < 0 || maxListCount > 0xFFFF)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        this.mMaxListCount = maxListCount;
    }

    public int getStartOffset() {
        return mStartOffset;
    }

    public void setStartOffset(int startOffset) throws IllegalArgumentException {
        if (startOffset < 0 || startOffset > 0xFFFF)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        this.mStartOffset = startOffset;
    }

    public int getFilterMessageType() {
        return mFilterMessageType;
    }

    public void setFilterMessageType(int filterMessageType) throws IllegalArgumentException {
        if (filterMessageType < 0 || filterMessageType > 0x000F)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x000F");
        this.mFilterMessageType = filterMessageType;
    }

    public long getFilterPeriodBegin() {
        return mFilterPeriodBegin;
    }

    public String getFilterPeriodBeginString() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        Date date = new Date(mFilterPeriodBegin);
        return format.format(date); // Format to YYYYMMDDTHHMMSS local time
    }

    public void setFilterPeriodBegin(long filterPeriodBegin) {
        this.mFilterPeriodBegin = filterPeriodBegin;
    }

    public void setFilterPeriodBegin(String filterPeriodBegin) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        Date date = format.parse(filterPeriodBegin);
        this.mFilterPeriodBegin = date.getTime();
    }

    public long getFilterPeriodEnd() {
        return mFilterPeriodEnd;
    }

    public String getFilterPeriodEndString() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        Date date = new Date(mFilterPeriodEnd);
        return format.format(date); // Format to YYYYMMDDTHHMMSS local time
    }

    public void setFilterPeriodEnd(long filterPeriodEnd) {
        this.mFilterPeriodEnd = filterPeriodEnd;
    }

    public void setFilterPeriodEnd(String filterPeriodEnd) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        Date date = format.parse(filterPeriodEnd);
        this.mFilterPeriodEnd = date.getTime();
    }

    public int getFilterReadStatus() {
        return mFilterReadStatus;
    }

    public void setFilterReadStatus(int filterReadStatus) throws IllegalArgumentException {
        if (filterReadStatus < 0 || filterReadStatus > 0x0002)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0002");
        this.mFilterReadStatus = filterReadStatus;
    }

    public String getFilterRecipient() {
        return mFilterRecipient;
    }

    public void setFilterRecipient(String filterRecipient) {
        this.mFilterRecipient = filterRecipient;
    }

    public String getFilterOriginator() {
        return mFilterOriginator;
    }

    public void setFilterOriginator(String filterOriginator) {
        this.mFilterOriginator = filterOriginator;
    }

    public int getFilterPriority() {
        return mFilterPriority;
    }

    public void setFilterPriority(int filterPriority) throws IllegalArgumentException {
        if (filterPriority < 0 || filterPriority > 0x0002)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0002");
        this.mFilterPriority = filterPriority;
    }

    public int getAttachment() {
        return mAttachment;
    }

    public void setAttachment(int attachment) throws IllegalArgumentException {
        if (attachment < 0 || attachment > 0x0001)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        this.mAttachment = attachment;
    }

    public int getTransparent() {
        return mTransparent;
    }

    public void setTransparent(int transparent) throws IllegalArgumentException {
        if (transparent < 0 || transparent > 0x0001)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        this.mTransparent = transparent;
    }

    public int getRetry() {
        return mRetry;
    }

    public void setRetry(int retry) throws IllegalArgumentException {
        if (retry < 0 || retry > 0x0001)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        this.mRetry = retry;
    }

    public int getNewMessage() {
        return mNewMessage;
    }

    public void setNewMessage(int newMessage) throws IllegalArgumentException {
        if (newMessage < 0 || newMessage > 0x0001)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        this.mNewMessage = newMessage;
    }

    public int getNotificationStatus() {
        return mNotificationStatus;
    }

    public void setNotificationStatus(int notificationStatus) throws IllegalArgumentException {
        if (notificationStatus < 0 || notificationStatus > 0x0001)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        this.mNotificationStatus = notificationStatus;
    }

    public int getMasInstanceId() {
        return mMasInstanceId;
    }

    public void setMasInstanceId(int masInstanceId) {
        if (masInstanceId < 0 || masInstanceId > 0x00FF)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x00FF");
        this.mMasInstanceId = masInstanceId;
    }

    public long getParameterMask() {
        return mParameterMask;
    }

    public void setParameterMask(long parameterMask) {
        if (parameterMask < 0 || parameterMask > 0xFFFFFFFFL)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFFFFFF");
        this.mParameterMask = parameterMask;
    }

    public int getFolderListingSize() {
        return mFolderListingSize;
    }

    public void setFolderListingSize(int folderListingSize) {
        if (folderListingSize < 0 || folderListingSize > 0xFFFF)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        this.mFolderListingSize = folderListingSize;
    }

    public int getMessageListingSize() {
        return mMessageListingSize;
    }

    public void setMessageListingSize(int messageListingSize) {
        if (messageListingSize < 0 || messageListingSize > 0xFFFF)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0xFFFF");
        this.mMessageListingSize = messageListingSize;
    }

    public int getSubjectLength() {
        return mSubjectLength;
    }

    public void setSubjectLength(int subjectLength) {
        if (subjectLength < 0 || subjectLength > 0xFF)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x00FF");
        this.mSubjectLength = subjectLength;
    }

    public int getCharset() {
        return mCharset;
    }

    public void setCharset(int charset) {
        if (charset < 0 || charset > 0x1)
            throw new IllegalArgumentException("Out of range: " + charset + ", valid range is 0x0000 to 0x0001");
        this.mCharset = charset;
    }

    public int getFractionRequest() {
        return mFractionRequest;
    }

    public void setFractionRequest(int fractionRequest) {
        if (fractionRequest < 0 || fractionRequest > 0x1)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        this.mFractionRequest = fractionRequest;
    }

    public int getFractionDeliver() {
        return mFractionDeliver;
    }

    public void setFractionDeliver(int fractionDeliver) {
        if (fractionDeliver < 0 || fractionDeliver > 0x1)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        this.mFractionDeliver = fractionDeliver;
    }

    public int getStatusIndicator() {
        return mStatusIndicator;
    }

    public void setStatusIndicator(int statusIndicator) {
        if (statusIndicator < 0 || statusIndicator > 0x1)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        this.mStatusIndicator = statusIndicator;
    }

    public int getStatusValue() {
        return mStatusValue;
    }

    public void setStatusValue(int statusValue) {
        if (statusValue < 0 || statusValue > 0x1)
            throw new IllegalArgumentException("Out of range, valid range is 0x0000 to 0x0001");
        this.mStatusValue = statusValue;
    }

    public long getMseTime() {
        return mMseTime;
    }

    public String getMseTimeString() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
        Date date = new Date(getMseTime());
        return format.format(date); // Format to YYYYMMDDTHHMMSS±hhmm UTC time ± offset
    }

    public void setMseTime(long mseTime) {
        this.mMseTime = mseTime;
    }

    public void setMseTime(String mseTime) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
        Date date = format.parse(mseTime);
        this.mMseTime = date.getTime();
    }
}
