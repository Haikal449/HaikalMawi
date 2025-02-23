/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright 2013, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "RTPSender"
#include <utils/Log.h>
#include "RTPSender.h"

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/ANetworkSession.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/Utils.h>

#include "include/avc_utils.h"

#ifdef MTK_AOSP_ENHANCEMENT
#include "DataPathTrace.h"

#ifdef MTB_SUPPORT
#define ATRACE_TAG ATRACE_TAG_MTK_WFD
#include <utils/Trace.h>
#endif
#endif

namespace android {

#ifdef MTK_AOSP_ENHANCEMENT



bool  refineSSRC(uint8_t* pRtpBuffer,uint32_t originalId,const sp<ABuffer> &Packets){
    bool isVideo = false;
    int32_t dummy=0;
    uint32_t id = originalId;

    if (Packets->meta()->findInt32("isVideo", &dummy) && dummy == 1) {
        isVideo = true;
        id = 0xdeadbeee ;//videoId
    }
  /*
    pRtpBuffer[8] = id >> 24;
    pRtpBuffer[9] = (id >> 16) & 0xff;
    pRtpBuffer[10] = (id >> 8) & 0xff;
    pRtpBuffer[11] = id & 0xff;
*/
    return  isVideo;

}
bool  isVideoBuffer(const sp<ABuffer> &buffer){

	bool isVideo = false;
	int32_t dummy=0;

	if (buffer->meta()->findInt32("isVideo", &dummy) && dummy == 1) {
		isVideo = true;
	}

	return  isVideo;

}
bool  isVideoDummy(const sp<ABuffer> &buffer){

	int32_t dummy;
	if( buffer->meta()->findInt32("dummy-nal", &dummy)){
		return true;
	}

	return false;
}


int64_t RTPSender::queuePackets_pre(
        const sp<ABuffer> &Packets){

    bool isVideo = false;
    int32_t dummy;
    int64_t timeUs;
    CHECK(Packets->meta()->findInt64("timeUs", &timeUs));


    if (Packets->meta()->findInt32("isVideo", &dummy) && dummy == 1) {
        isVideo = true;
        mVideoCount ++;
    }

    int64_t delayUs = -1;
    int64_t whenUs = -1;

    if (mFirstOutputBufferReadyTimeUs < 0ll) {
         if ((isVideo == true) && (mVideoCount == 100))
         {
             mFirstOutputBufferReadyTimeUs = timeUs;
             mFirstOutputBufferSentTimeUs = whenUs = ALooper::GetNowUs();
             delayUs = 0ll;
         }
    } else {
        int64_t nowUs = ALooper::GetNowUs();

        whenUs = (timeUs - mFirstOutputBufferReadyTimeUs)
                + mFirstOutputBufferSentTimeUs;

        delayUs =  nowUs -whenUs ;
    }
    calcSendInBitRate(Packets);
    return delayUs;

}

void RTPSender::queuePackets_pro(
        const sp<ABuffer> &Packets, int64_t timeUs,int64_t latencyB,int64_t startSendUs,int64_t /*delayUs*/){


	int64_t endTimeUs = ALooper::GetNowUs();
	int64_t netTimeUs = endTimeUs - startSendUs;

	static int sendFps=-1;

	bool  isVideo = isVideoBuffer(Packets);
	bool isDummyVideo = isVideoDummy(Packets);

	if(isVideo)
	{
		int64_t nowUsFps = 0;
		static int64_t mStartSysTime = 0;
		static int mCountFrames = 0;
		int mCountFramerate;

		//count framerate.
		if(((mCountFrames % 60) == 0) && (mCountFrames != 0))
		{
			nowUsFps = endTimeUs;
			mCountFramerate = (mCountFrames * 1000 * 1000) / (nowUsFps - mStartSysTime);
			mCountFrames = 0;
			mStartSysTime = nowUsFps;
			sendFps = mCountFramerate;
		}
		int32_t dummy;
		if(!isDummyVideo){
			mCountFrames ++;

		}

	}


    sp<WfdDebugInfo> debugInfo= defaultWfdDebugInfo();
  /*
    if(!Packets->meta()->findInt32("dummy-nal", &dummy)){
        debugInfo->addTimeInfoByKey(isVideo , timeUs, "StIn", startSendUs/1000);
        debugInfo->addTimeInfoByKey(isVideo , timeUs, "StOt", endTimeUs/1000);
    }


	int64_t st = debugInfo->getTimeInfoByKey(  isVideo,   timeUs, isVideo?"RpIn":"MpIn");
	int64_t et = debugInfo->getTimeInfoByKey(  isVideo,   timeUs, "StOt");

	int64_t latencyTimeMs=-1;
	if(st >0 && et >0 ){
		latencyTimeMs = et -st;
		debugInfo->addTimeInfoByKey(isVideo , timeUs, "Latency", latencyTimeMs);
	}
  */
  	int64_t latencyTimeMs=-1;
	if(latencyB > 0){
		latencyTimeMs  =   endTimeUs/1000ll - latencyB ;
	}
/*
	ALOGI("[WFD_P][%s][dummy=%d]ts=%lld ms,size=%-6d,[SendFPS] =[%-3d][netTimeUs]=%-4lldms,[delayUs]=%-4lldms ,[Latency]=%-6lldms",
			isVideo ? "video" : "audio",isDummyVideo,timeUs/1000ll,Packets->size(),sendFps,netTimeUs/1000ll,delayUs/1000ll,latencyTimeMs);

    //Rock, Remove log in L
*/
	debugInfo->printDebugInfoByKey( isVideo,   timeUs);

#ifdef MTB_SUPPORT
    uint32_t type = isVideo ? (ATRACE_ONESHOT_VDATA) : (ATRACE_ONESHOT_ADATA);
    ATRACE_ONESHOT(type, "RTPSender, TS: %lld ms", timeUs/1000);
#endif

}


status_t RTPSender::calcSendInBitRate(const sp<ABuffer> &buffer){
    int64_t SendTimeUs = ALooper::GetNowUs();
    mSentInHistory.add(SendTimeUs, buffer);

    int64_t beginTimeUs = mSentInHistory.keyAt(0);
    int64_t endTimeUs = SendTimeUs;

    int64_t sendTotalTimeUs = endTimeUs - beginTimeUs  ;


    if(sendTotalTimeUs > 1000000ll){//once a second

        int64_t totalSize =0;
        int64_t totalVideoSize =0;
        int64_t st =0;
        int64_t et =0;

        for(uint32_t i =0 ;i < mSentInHistory.size() ;i++){
            int32_t dummy =-1;
            sp<ABuffer> curBuffer = mSentInHistory.editValueAt(i);
            if ((curBuffer->meta()->findInt32("isVideo", &dummy)) &&  (dummy ==1)) {
                totalVideoSize += buffer->size();
            }
            totalSize += buffer->size();
        }

        mSentInbps[0] = totalSize*1000000ll/sendTotalTimeUs;
        mSentInbps[1] = totalVideoSize*1000000ll/sendTotalTimeUs;
        mSentInbps[2] = (totalSize-totalVideoSize)*1000000ll/sendTotalTimeUs;

        mSentInHistory.clear();
	/*
	ALOGI("[WFD_P]kBps:T:[%-6lld vs %-6lld],V:[%-6lld vs %-6lld],,A:[%-6lld vs %-6lld]",
		mSentInbps[0]/1024ll,mSentRtpbps[0]/1024ll,
		mSentInbps[1]/1024ll,mSentRtpbps[1]/1024ll,
		mSentInbps[2]/1024ll,mSentRtpbps[2]/1024ll);

	ALOGI("[WFD_P]kBps:T: %d,V: %d, A: %d",
	mSentInbps[0]/1024ll,mSentInbps[1]/1024ll,mSentInbps[2]/1024l);
    //Rock ,remvoe Log in L
	*/


    }
    return OK;
}


status_t RTPSender::calcSendRtpBitRate(const sp<ABuffer> &buffer){
    int64_t SendTimeUs = ALooper::GetNowUs();
    mSentRtpHistory.add(SendTimeUs, buffer);

    int64_t beginTimeUs = mSentRtpHistory.keyAt(0);
    int64_t endTimeUs = SendTimeUs;

    int64_t sendTotalTimeUs = endTimeUs - beginTimeUs  ;


    if(sendTotalTimeUs > 1000000ll){//once a second

        int64_t totalSize =0;
        int64_t totalVideoSize =0;
        int64_t st =0;
        int64_t et =0;

        for(uint32_t i =0 ;i < mSentRtpHistory.size() ;i++){
            int32_t dummy =-1;
            sp<ABuffer> curBuffer = mSentRtpHistory.editValueAt(i);
            if ((curBuffer->meta()->findInt32("isVideo", &dummy)) &&  (dummy ==1)) {
                totalVideoSize += buffer->size();
            }
            totalSize += buffer->size();
        }

        mSentRtpbps[0] = totalSize*1000000ll/sendTotalTimeUs;
        mSentRtpbps[1] = totalVideoSize*1000000ll/sendTotalTimeUs;
        mSentRtpbps[2] = (totalSize-totalVideoSize)*1000000ll/sendTotalTimeUs;

        mSentRtpHistory.clear();

    }
    return OK;
}




status_t RTPSender::sendRTPPackets(
       List<sp<ABuffer> > &packets ,int64_t timeUs) {

    	CHECK(mRTPConnected);

   	 status_t err = mNetSession->sendWFDRequest(
            mRTPSessionID, packets,timeUs);

	if (err != OK) {
		return err;
	}
	/*
	List<sp<ABuffer> >::iterator it = packets.begin();
	while (it != packets.end()) {
		const sp<ABuffer> &buffer = *it;
		calcSendRtpBitRate(buffer);
	}*/

    	return OK;
}



#endif
RTPSender::RTPSender(
        const sp<ANetworkSession> &netSession,
        const sp<AMessage> &notify)
    : mNetSession(netSession),
      mNotify(notify),
      mRTPMode(TRANSPORT_UNDEFINED),
      mRTCPMode(TRANSPORT_UNDEFINED),
      mRTPSessionID(0),
      mRTCPSessionID(0),
      mRTPConnected(false),
      mRTCPConnected(false),
      mLastNTPTime(0),
      mLastRTPTime(0),
      mNumRTPSent(0),
      mNumRTPOctetsSent(0),
      mNumSRsSent(0),
      mRTPSeqNo(0),
      mHistorySize(0) {
#ifdef MTK_AOSP_ENHANCEMENT
      mFirstOutputBufferReadyTimeUs = -1;
      mFirstOutputBufferSentTimeUs = -1;
      mVideoCount = 0;
      mSentRtpbps[0]= -1;
      mSentRtpbps[1]= -1;
      mSentRtpbps[2]= -1;
      mSentInbps[0]= -1;
      mSentInbps[1]= -1;
      mSentInbps[2]= -1;
#endif

}

RTPSender::~RTPSender() {
    if (mRTCPSessionID != 0) {
        mNetSession->destroySession(mRTCPSessionID);
        mRTCPSessionID = 0;
    }

    if (mRTPSessionID != 0) {
        mNetSession->destroySession(mRTPSessionID);
        mRTPSessionID = 0;
    }
}

// static
int32_t RTPBase::PickRandomRTPPort() {
    // Pick an even integer in range [1024, 65534)

    static const size_t kRange = (65534 - 1024) / 2;

    return (int32_t)(((float)(kRange + 1) * rand()) / RAND_MAX) * 2 + 1024;
}

status_t RTPSender::initAsync(
        const char *remoteHost,
        int32_t remoteRTPPort,
        TransportMode rtpMode,
        int32_t remoteRTCPPort,
        TransportMode rtcpMode,
        int32_t *outLocalRTPPort) {
    if (mRTPMode != TRANSPORT_UNDEFINED
            || rtpMode == TRANSPORT_UNDEFINED
            || rtpMode == TRANSPORT_NONE
            || rtcpMode == TRANSPORT_UNDEFINED) {
        return INVALID_OPERATION;
    }

    CHECK_NE(rtpMode, TRANSPORT_TCP_INTERLEAVED);
    CHECK_NE(rtcpMode, TRANSPORT_TCP_INTERLEAVED);

    if ((rtcpMode == TRANSPORT_NONE && remoteRTCPPort >= 0)
            || (rtcpMode != TRANSPORT_NONE && remoteRTCPPort < 0)) {
        return INVALID_OPERATION;
    }

    sp<AMessage> rtpNotify = new AMessage(kWhatRTPNotify, id());

    sp<AMessage> rtcpNotify;
    if (remoteRTCPPort >= 0) {
        rtcpNotify = new AMessage(kWhatRTCPNotify, id());
    }

    CHECK_EQ(mRTPSessionID, 0);
    CHECK_EQ(mRTCPSessionID, 0);

    int32_t localRTPPort;

    for (;;) {
        localRTPPort = PickRandomRTPPort();

        status_t err;
        if (rtpMode == TRANSPORT_UDP) {
            err = mNetSession->createUDPSession(
                    localRTPPort,
                    remoteHost,
                    remoteRTPPort,
                    rtpNotify,
                    &mRTPSessionID);
        } else {
            CHECK_EQ(rtpMode, TRANSPORT_TCP);
            err = mNetSession->createTCPDatagramSession(
                    localRTPPort,
                    remoteHost,
                    remoteRTPPort,
                    rtpNotify,
                    &mRTPSessionID);
        }

        if (err != OK) {
            continue;
        }

        if (remoteRTCPPort < 0) {
            break;
        }

        if (rtcpMode == TRANSPORT_UDP) {
            err = mNetSession->createUDPSession(
                    localRTPPort + 1,
                    remoteHost,
                    remoteRTCPPort,
                    rtcpNotify,
                    &mRTCPSessionID);
        } else {
            CHECK_EQ(rtcpMode, TRANSPORT_TCP);
            err = mNetSession->createTCPDatagramSession(
                    localRTPPort + 1,
                    remoteHost,
                    remoteRTCPPort,
                    rtcpNotify,
                    &mRTCPSessionID);
        }

        if (err == OK) {
            break;
        }

        mNetSession->destroySession(mRTPSessionID);
        mRTPSessionID = 0;
    }

    if (rtpMode == TRANSPORT_UDP) {
        mRTPConnected = true;
    }

    if (rtcpMode == TRANSPORT_UDP) {
        mRTCPConnected = true;
    }

    mRTPMode = rtpMode;
    mRTCPMode = rtcpMode;
    *outLocalRTPPort = localRTPPort;

    if (mRTPMode == TRANSPORT_UDP
            && (mRTCPMode == TRANSPORT_UDP || mRTCPMode == TRANSPORT_NONE)) {
        notifyInitDone(OK);
    }

    return OK;
}

status_t RTPSender::queueBuffer(
        const sp<ABuffer> &buffer, uint8_t packetType, PacketizationMode mode) {
    status_t err;

    switch (mode) {
        case PACKETIZATION_NONE:
            err = queueRawPacket(buffer, packetType);
            break;

        case PACKETIZATION_TRANSPORT_STREAM:
            err = queueTSPackets(buffer, packetType);
            break;

        case PACKETIZATION_H264:
            err  = queueAVCBuffer(buffer, packetType);
            break;

        default:
            TRESPASS();
    }

    return err;
}

status_t RTPSender::queueRawPacket(
        const sp<ABuffer> &packet, uint8_t packetType) {
    CHECK_LE(packet->size(), kMaxUDPPacketSize - 12);

    int64_t timeUs;
    CHECK(packet->meta()->findInt64("timeUs", &timeUs));

    sp<ABuffer> udpPacket = new ABuffer(12 + packet->size());

    udpPacket->setInt32Data(mRTPSeqNo);

    uint8_t *rtp = udpPacket->data();
    rtp[0] = 0x80;
    rtp[1] = packetType;

    rtp[2] = (mRTPSeqNo >> 8) & 0xff;
    rtp[3] = mRTPSeqNo & 0xff;
    ++mRTPSeqNo;

    uint32_t rtpTime = (timeUs * 9) / 100ll;

    rtp[4] = rtpTime >> 24;
    rtp[5] = (rtpTime >> 16) & 0xff;
    rtp[6] = (rtpTime >> 8) & 0xff;
    rtp[7] = rtpTime & 0xff;

    rtp[8] = kSourceID >> 24;
    rtp[9] = (kSourceID >> 16) & 0xff;
    rtp[10] = (kSourceID >> 8) & 0xff;
    rtp[11] = kSourceID & 0xff;

    memcpy(&rtp[12], packet->data(), packet->size());

    return sendRTPPacket(
            udpPacket,
            true /* storeInHistory */,
            true /* timeValid */,
            ALooper::GetNowUs());
}

status_t RTPSender::queueTSPackets(
        const sp<ABuffer> &tsPackets, uint8_t packetType) {
    CHECK_EQ(0, tsPackets->size() % 188);

    int64_t timeUs;
    CHECK(tsPackets->meta()->findInt64("timeUs", &timeUs));

#ifdef MTK_AOSP_ENHANCEMENT
    int64_t delayUs = queuePackets_pre(tsPackets);
    int64_t startSendUs = ALooper::GetNowUs();
    bool  isVideo = isVideoBuffer(tsPackets);
    bool isDummyVideo = isVideoDummy(tsPackets);

    sp<WfdDebugInfo> debugInfo= defaultWfdDebugInfo();
    int64_t latencyB = -1;
    int64_t LatencyToken = -1;
    if(isDummyVideo){
	 tsPackets->meta()->findInt64("latencyB", &latencyB);
	 tsPackets->meta()->findInt64("LatencyToken", &LatencyToken);
    }else{
	latencyB= debugInfo->getTimeInfoByKey(  isVideo,   timeUs, isVideo?"RpIn":"MpIn");
    }

     List<sp<ABuffer> > mAllPackets ;
     bool isFirstUdp = true;
#endif

    const size_t numTSPackets = tsPackets->size() / 188;

    size_t srcOffset = 0;

    while (srcOffset < tsPackets->size()) {
        sp<ABuffer> udpPacket =
            new ABuffer(12 + kMaxNumTSPacketsPerRTPPacket * 188);

        udpPacket->setInt32Data(mRTPSeqNo);

        uint8_t *rtp = udpPacket->data();
        rtp[0] = 0x80;
        rtp[1] = packetType;

        rtp[2] = (mRTPSeqNo >> 8) & 0xff;
        rtp[3] = mRTPSeqNo & 0xff;
        ++mRTPSeqNo;

        int64_t nowUs = ALooper::GetNowUs();
        uint32_t rtpTime = (nowUs * 9) / 100ll;
#ifdef MTK_AOSP_ENHANCEMENT
        rtpTime = (timeUs * 9ll) / 100ll;
#endif
        rtp[4] = rtpTime >> 24;
        rtp[5] = (rtpTime >> 16) & 0xff;
        rtp[6] = (rtpTime >> 8) & 0xff;
        rtp[7] = rtpTime & 0xff;

        rtp[8] = kSourceID >> 24;
        rtp[9] = (kSourceID >> 16) & 0xff;
        rtp[10] = (kSourceID >> 8) & 0xff;
        rtp[11] = kSourceID & 0xff;

#ifdef MTK_AOSP_ENHANCEMENT
        bool isVideo = refineSSRC(rtp,kSourceID,tsPackets);//just used for reconignizing video/audio now
#endif

        size_t numTSPackets = (tsPackets->size() - srcOffset) / 188;
        if (numTSPackets > kMaxNumTSPacketsPerRTPPacket) {
            numTSPackets = kMaxNumTSPacketsPerRTPPacket;
        }

        memcpy(&rtp[12], tsPackets->data() + srcOffset, numTSPackets * 188);

        udpPacket->setRange(0, 12 + numTSPackets * 188);

        srcOffset += numTSPackets * 188;
        bool isLastPacket = (srcOffset == tsPackets->size());

#ifdef MTK_AOSP_ENHANCEMENT
        //set MarketBit to indicate the timestamp discontiuous
        rtp[1] |= 0x80 & ((isLastPacket?1:0) << 7);
				udpPacket->meta()->setInt32("isVideo", isVideo?1:0);
				udpPacket->meta()->setInt32("isDummy", isDummyVideo?1:0);
				udpPacket->meta()->setInt32("isLast", isLastPacket?1:0);
				udpPacket->meta()->setInt64("latencyB", latencyB);
				udpPacket->meta()->setInt64("LatencyToken", LatencyToken);
				udpPacket->meta()->setInt32("isFirst", isFirstUdp? 1:0);
			  isFirstUdp =false;
				mAllPackets.push_back(udpPacket);
#endif

#ifndef MTK_AOSP_ENHANCEMENT
	status_t err = sendRTPPacket(
                udpPacket,
                true /* storeInHistory */,
                isLastPacket /* timeValid */,
                timeUs);

        if (err != OK) {
            return err;
        }

#endif

    }
#ifdef MTK_AOSP_ENHANCEMENT
	status_t err = sendRTPPackets(mAllPackets, timeUs);

        if (err != OK) {
            return err;
        }
     queuePackets_pro(tsPackets,timeUs, latencyB,startSendUs,delayUs);
#endif
    return OK;
}

status_t RTPSender::queueAVCBuffer(
        const sp<ABuffer> &accessUnit, uint8_t packetType) {
    int64_t timeUs;
    CHECK(accessUnit->meta()->findInt64("timeUs", &timeUs));

    uint32_t rtpTime = (timeUs * 9 / 100ll);

    List<sp<ABuffer> > packets;

    sp<ABuffer> out = new ABuffer(kMaxUDPPacketSize);
    size_t outBytesUsed = 12;  // Placeholder for RTP header.

    const uint8_t *data = accessUnit->data();
    size_t size = accessUnit->size();
    const uint8_t *nalStart;
    size_t nalSize;
    while (getNextNALUnit(
                &data, &size, &nalStart, &nalSize,
                true /* startCodeFollows */) == OK) {
        size_t bytesNeeded = nalSize + 2;
        if (outBytesUsed == 12) {
            ++bytesNeeded;
        }

        if (outBytesUsed + bytesNeeded > out->capacity()) {
            bool emitSingleNALPacket = false;

            if (outBytesUsed == 12
                    && outBytesUsed + nalSize <= out->capacity()) {
                // We haven't emitted anything into the current packet yet and
                // this NAL unit fits into a single-NAL-unit-packet while
                // it wouldn't have fit as part of a STAP-A packet.

                memcpy(out->data() + outBytesUsed, nalStart, nalSize);
                outBytesUsed += nalSize;

                emitSingleNALPacket = true;
            }

            if (outBytesUsed > 12) {
                out->setRange(0, outBytesUsed);
                packets.push_back(out);
                out = new ABuffer(kMaxUDPPacketSize);
                outBytesUsed = 12;  // Placeholder for RTP header
            }

            if (emitSingleNALPacket) {
                continue;
            }
        }

        if (outBytesUsed + bytesNeeded <= out->capacity()) {
            uint8_t *dst = out->data() + outBytesUsed;

            if (outBytesUsed == 12) {
                *dst++ = 24;  // STAP-A header
            }

            *dst++ = (nalSize >> 8) & 0xff;
            *dst++ = nalSize & 0xff;
            memcpy(dst, nalStart, nalSize);

            outBytesUsed += bytesNeeded;
            continue;
        }

        // This single NAL unit does not fit into a single RTP packet,
        // we need to emit an FU-A.

        CHECK_EQ(outBytesUsed, 12u);

        uint8_t nalType = nalStart[0] & 0x1f;
        uint8_t nri = (nalStart[0] >> 5) & 3;

        size_t srcOffset = 1;
        while (srcOffset < nalSize) {
            size_t copy = out->capacity() - outBytesUsed - 2;
            if (copy > nalSize - srcOffset) {
                copy = nalSize - srcOffset;
            }

            uint8_t *dst = out->data() + outBytesUsed;
            dst[0] = (nri << 5) | 28;

            dst[1] = nalType;

            if (srcOffset == 1) {
                dst[1] |= 0x80;
            }

            if (srcOffset + copy == nalSize) {
                dst[1] |= 0x40;
            }

            memcpy(&dst[2], nalStart + srcOffset, copy);
            srcOffset += copy;

            out->setRange(0, outBytesUsed + copy + 2);

            packets.push_back(out);
            out = new ABuffer(kMaxUDPPacketSize);
            outBytesUsed = 12;  // Placeholder for RTP header
        }
    }

    if (outBytesUsed > 12) {
        out->setRange(0, outBytesUsed);
        packets.push_back(out);
    }

    while (!packets.empty()) {
        sp<ABuffer> out = *packets.begin();
        packets.erase(packets.begin());

        out->setInt32Data(mRTPSeqNo);

        bool last = packets.empty();

        uint8_t *dst = out->data();

        dst[0] = 0x80;

        dst[1] = packetType;
        if (last) {
            dst[1] |= 1 << 7;  // M-bit
        }

        dst[2] = (mRTPSeqNo >> 8) & 0xff;
        dst[3] = mRTPSeqNo & 0xff;
        ++mRTPSeqNo;

        dst[4] = rtpTime >> 24;
        dst[5] = (rtpTime >> 16) & 0xff;
        dst[6] = (rtpTime >> 8) & 0xff;
        dst[7] = rtpTime & 0xff;
        dst[8] = kSourceID >> 24;
        dst[9] = (kSourceID >> 16) & 0xff;
        dst[10] = (kSourceID >> 8) & 0xff;
        dst[11] = kSourceID & 0xff;

        status_t err = sendRTPPacket(out, true /* storeInHistory */);

        if (err != OK) {
            return err;
        }
    }

    return OK;
}

status_t RTPSender::sendRTPPacket(
         const sp<ABuffer> &buffer  , bool storeInHistory,
        bool timeValid, int64_t timeUs) {

    CHECK(mRTPConnected);

    status_t err = mNetSession->sendRequest(
            mRTPSessionID, buffer->data(), buffer->size(),
            timeValid, timeUs);

    if (err != OK) {
        return err;
    }

    mLastNTPTime = GetNowNTP();
    mLastRTPTime = U32_AT(buffer->data() + 4);

    ++mNumRTPSent;
    mNumRTPOctetsSent += buffer->size() - 12;


    if (storeInHistory) {
        if (mHistorySize == kMaxHistorySize) {
            mHistory.erase(mHistory.begin());
        } else {
            ++mHistorySize;
        }
        mHistory.push_back(buffer);
    }

#ifdef MTK_AOSP_ENHANCEMENT
    calcSendRtpBitRate(buffer);
#endif
    return OK;
}

// static
uint64_t RTPSender::GetNowNTP() {
    struct timeval tv;
    gettimeofday(&tv, NULL /* timezone */);

    uint64_t nowUs = tv.tv_sec * 1000000ll + tv.tv_usec;

    nowUs += ((70ll * 365 + 17) * 24) * 60 * 60 * 1000000ll;

    uint64_t hi = nowUs / 1000000ll;
    uint64_t lo = ((1ll << 32) * (nowUs % 1000000ll)) / 1000000ll;

    return (hi << 32) | lo;
}

void RTPSender::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatRTPNotify:
        case kWhatRTCPNotify:
            onNetNotify(msg->what() == kWhatRTPNotify, msg);
            break;

        default:
            TRESPASS();
    }
}

void RTPSender::onNetNotify(bool isRTP, const sp<AMessage> &msg) {
    int32_t reason;
    CHECK(msg->findInt32("reason", &reason));

    switch (reason) {
        case ANetworkSession::kWhatError:
        {
            int32_t sessionID;
            CHECK(msg->findInt32("sessionID", &sessionID));

            int32_t err;
            CHECK(msg->findInt32("err", &err));

            int32_t errorOccuredDuringSend;
            CHECK(msg->findInt32("send", &errorOccuredDuringSend));

            AString detail;
            CHECK(msg->findString("detail", &detail));

            ALOGE("An error occurred during %s in session %d "
                  "(%d, '%s' (%s)).",
                  errorOccuredDuringSend ? "send" : "receive",
                  sessionID,
                  err,
                  detail.c_str(),
                  strerror(-err));

            mNetSession->destroySession(sessionID);

            if (sessionID == mRTPSessionID) {
                mRTPSessionID = 0;
            } else if (sessionID == mRTCPSessionID) {
                mRTCPSessionID = 0;
            }

            if (!mRTPConnected
                    || (mRTPMode != TRANSPORT_NONE && !mRTCPConnected)) {
                // We haven't completed initialization, attach the error
                // to the notification instead.
                notifyInitDone(err);
                break;
            }

            notifyError(err);
            break;
        }

        case ANetworkSession::kWhatDatagram:
        {
            sp<ABuffer> data;
            CHECK(msg->findBuffer("data", &data));

            if (isRTP) {
                ALOGW("Huh? Received data on RTP connection...");
            } else {
                onRTCPData(data);
            }
            break;
        }

        case ANetworkSession::kWhatConnected:
        {
            int32_t sessionID;
            CHECK(msg->findInt32("sessionID", &sessionID));

            if  (isRTP) {
                CHECK_EQ(mRTPMode, TRANSPORT_TCP);
                CHECK_EQ(sessionID, mRTPSessionID);
                mRTPConnected = true;
            } else {
                CHECK_EQ(mRTCPMode, TRANSPORT_TCP);
                CHECK_EQ(sessionID, mRTCPSessionID);
                mRTCPConnected = true;
            }

            if (mRTPConnected
                    && (mRTCPMode == TRANSPORT_NONE || mRTCPConnected)) {
                notifyInitDone(OK);
            }
            break;
        }

        case ANetworkSession::kWhatNetworkStall:
        {
            size_t numBytesQueued;
            CHECK(msg->findSize("numBytesQueued", &numBytesQueued));

            notifyNetworkStall(numBytesQueued);
            break;
        }

        default:
            TRESPASS();
    }
}

status_t RTPSender::onRTCPData(const sp<ABuffer> &buffer) {
    const uint8_t *data = buffer->data();
    size_t size = buffer->size();

    while (size > 0) {
        if (size < 8) {
            // Too short to be a valid RTCP header
            return ERROR_MALFORMED;
        }

        if ((data[0] >> 6) != 2) {
            // Unsupported version.
            return ERROR_UNSUPPORTED;
        }

        if (data[0] & 0x20) {
            // Padding present.

            size_t paddingLength = data[size - 1];

            if (paddingLength + 12 > size) {
                // If we removed this much padding we'd end up with something
                // that's too short to be a valid RTP header.
                return ERROR_MALFORMED;
            }

            size -= paddingLength;
        }

        size_t headerLength = 4 * (data[2] << 8 | data[3]) + 4;

        if (size < headerLength) {
            // Only received a partial packet?
            return ERROR_MALFORMED;
        }

        switch (data[1]) {
            case 200:
            case 201:  // RR
                parseReceiverReport(data, headerLength);
                break;

            case 202:  // SDES
            case 203:
                break;

            case 204:  // APP
                parseAPP(data, headerLength);
                break;

            case 205:  // TSFB (transport layer specific feedback)
                parseTSFB(data, headerLength);
                break;

            case 206:  // PSFB (payload specific feedback)
                // hexdump(data, headerLength);
                break;

            default:
            {
                ALOGW("Unknown RTCP packet type %u of size %d",
                     (unsigned)data[1], headerLength);
                break;
            }
        }

        data += headerLength;
        size -= headerLength;
    }

    return OK;
}

status_t RTPSender::parseReceiverReport(
        const uint8_t *data, size_t /* size */) {
    float fractionLost = data[12] / 256.0f;

    ALOGI("lost %.2f %% of packets during report interval.",
          100.0f * fractionLost);

    return OK;
}

status_t RTPSender::parseTSFB(const uint8_t *data, size_t size) {
    if ((data[0] & 0x1f) != 1) {
        return ERROR_UNSUPPORTED;  // We only support NACK for now.
    }

    uint32_t srcId = U32_AT(&data[8]);
    if (srcId != kSourceID) {
        return ERROR_MALFORMED;
    }

    for (size_t i = 12; i < size; i += 4) {
        uint16_t seqNo = U16_AT(&data[i]);
        uint16_t blp = U16_AT(&data[i + 2]);

        List<sp<ABuffer> >::iterator it = mHistory.begin();
        bool foundSeqNo = false;
        while (it != mHistory.end()) {
            const sp<ABuffer> &buffer = *it;

            uint16_t bufferSeqNo = buffer->int32Data() & 0xffff;

            bool retransmit = false;
            if (bufferSeqNo == seqNo) {
                retransmit = true;
            } else if (blp != 0) {
                for (size_t i = 0; i < 16; ++i) {
                    if ((blp & (1 << i))
                        && (bufferSeqNo == ((seqNo + i + 1) & 0xffff))) {
                        blp &= ~(1 << i);
                        retransmit = true;
                    }
                }
            }

            if (retransmit) {
                ALOGV("retransmitting seqNo %d", bufferSeqNo);

                CHECK_EQ((status_t)OK,
                         sendRTPPacket(buffer, false /* storeInHistory */));

                if (bufferSeqNo == seqNo) {
                    foundSeqNo = true;
                }

                if (foundSeqNo && blp == 0) {
                    break;
                }
            }

            ++it;
        }

        if (!foundSeqNo || blp != 0) {
            ALOGI("Some sequence numbers were no longer available for "
                  "retransmission (seqNo = %d, foundSeqNo = %d, blp = 0x%04x)",
                  seqNo, foundSeqNo, blp);

            if (!mHistory.empty()) {
                int32_t earliest = (*mHistory.begin())->int32Data() & 0xffff;
                int32_t latest = (*--mHistory.end())->int32Data() & 0xffff;

                ALOGI("have seq numbers from %d - %d", earliest, latest);
            }
        }
    }

    return OK;
}

status_t RTPSender::parseAPP(const uint8_t *data, size_t size) {
	static const size_t late_offset = 8;
    static const char late_string[] = "late";
    static const size_t avgLatencyUs_offset = late_offset + sizeof(late_string) - 1;
    static const size_t maxLatencyUs_offset = avgLatencyUs_offset + sizeof(int64_t);

    if ((size >= (maxLatencyUs_offset + sizeof(int64_t)))
            && !memcmp(late_string, &data[late_offset], sizeof(late_string) - 1)) {
        int64_t avgLatencyUs = (int64_t)U64_AT(&data[avgLatencyUs_offset]);
        int64_t maxLatencyUs = (int64_t)U64_AT(&data[maxLatencyUs_offset]);


        sp<AMessage> notify = mNotify->dup();
        notify->setInt32("what", kWhatInformSender);
        notify->setInt64("avgLatencyUs", avgLatencyUs);
        notify->setInt64("maxLatencyUs", maxLatencyUs);
        notify->post();
    }

    return OK;
}

void RTPSender::notifyInitDone(status_t err) {
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", kWhatInitDone);
    notify->setInt32("err", err);
    notify->post();
}

void RTPSender::notifyError(status_t err) {
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", kWhatError);
    notify->setInt32("err", err);
    notify->post();
}

void RTPSender::notifyNetworkStall(size_t numBytesQueued) {
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", kWhatNetworkStall);
    notify->setSize("numBytesQueued", numBytesQueued);
    notify->post();
}

}  // namespace android

