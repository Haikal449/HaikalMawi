/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (c) 2009 The Android Open Source Project
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
#define LOG_TAG "IOMX"
#include <utils/Log.h>

#include <sys/mman.h>

#include <binder/IMemory.h>
#include <binder/Parcel.h>
#include <media/IOMX.h>
#include <media/stagefright/foundation/ADebug.h>

namespace android {

enum {
    CONNECT = IBinder::FIRST_CALL_TRANSACTION,
    LIVES_LOCALLY,
    LIST_NODES,
    ALLOCATE_NODE,
    FREE_NODE,
    SEND_COMMAND,
    GET_PARAMETER,
    SET_PARAMETER,
    GET_CONFIG,
    SET_CONFIG,
    GET_STATE,
    ENABLE_GRAPHIC_BUFFERS,
    USE_BUFFER,
    USE_GRAPHIC_BUFFER,
    CREATE_INPUT_SURFACE,
    SIGNAL_END_OF_INPUT_STREAM,
    STORE_META_DATA_IN_BUFFERS,
    PREPARE_FOR_ADAPTIVE_PLAYBACK,
    ALLOC_BUFFER,
    ALLOC_BUFFER_WITH_BACKUP,
    FREE_BUFFER,
    FILL_BUFFER,
    EMPTY_BUFFER,
    GET_EXTENSION_INDEX,
    OBSERVER_ON_MSG,
    GET_GRAPHIC_BUFFER_USAGE,
    SET_INTERNAL_OPTION,
    UPDATE_GRAPHIC_BUFFER_IN_META,
    CONFIGURE_VIDEO_TUNNEL_MODE,
};

class BpOMX : public BpInterface<IOMX> {
public:
    BpOMX(const sp<IBinder> &impl)
        : BpInterface<IOMX>(impl) {
    }

    virtual bool livesLocally(node_id node, pid_t pid) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(pid);
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(LIVES_LOCALLY, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(LIVES_LOCALLY, data, &reply);
#endif
        return reply.readInt32() != 0;
    }

    virtual status_t listNodes(List<ComponentInfo> *list) {
        list->clear();

        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(LIST_NODES, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(LIST_NODES, data, &reply);
#endif
        int32_t n = reply.readInt32();
        for (int32_t i = 0; i < n; ++i) {
            list->push_back(ComponentInfo());
            ComponentInfo &info = *--list->end();

            info.mName = reply.readString8();
            int32_t numRoles = reply.readInt32();
            for (int32_t j = 0; j < numRoles; ++j) {
                info.mRoles.push_back(reply.readString8());
            }
        }

        return OK;
    }

    virtual status_t allocateNode(
            const char *name, const sp<IOMXObserver> &observer, node_id *node) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeCString(name);
        data.writeStrongBinder(observer->asBinder());
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(ALLOCATE_NODE, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(ALLOCATE_NODE, data, &reply);
#endif

        status_t err = reply.readInt32();
        if (err == OK) {
            *node = (node_id)reply.readInt32();
        } else {
            *node = 0;
        }

        return err;
    }

    virtual status_t freeNode(node_id node) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(FREE_NODE, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else  
        remote()->transact(FREE_NODE, data, &reply);
#endif
        return reply.readInt32();
    }

    virtual status_t sendCommand(
            node_id node, OMX_COMMANDTYPE cmd, OMX_S32 param) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(cmd);
        data.writeInt32(param);
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(SEND_COMMAND, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(SEND_COMMAND, data, &reply);
#endif
        return reply.readInt32();
    }

    virtual status_t getParameter(
            node_id node, OMX_INDEXTYPE index,
            void *params, size_t size) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(index);
        data.writeInt64(size);
        data.write(params, size);
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(GET_PARAMETER, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(GET_PARAMETER, data, &reply);
#endif        

        status_t err = reply.readInt32();
        if (err != OK) {
            return err;
        }

        reply.read(params, size);

        return OK;
    }

    virtual status_t setParameter(
            node_id node, OMX_INDEXTYPE index,
            const void *params, size_t size) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(index);
        data.writeInt64(size);
        data.write(params, size);
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(SET_PARAMETER, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else  
        remote()->transact(SET_PARAMETER, data, &reply);
#endif
        return reply.readInt32();
    }

    virtual status_t getConfig(
            node_id node, OMX_INDEXTYPE index,
            void *params, size_t size) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(index);
        data.writeInt64(size);
        data.write(params, size);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(GET_CONFIG, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else   
        remote()->transact(GET_CONFIG, data, &reply);
#endif
        status_t err = reply.readInt32();
        if (err != OK) {
            return err;
        }

        reply.read(params, size);

        return OK;
    }

    virtual status_t setConfig(
            node_id node, OMX_INDEXTYPE index,
            const void *params, size_t size) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(index);
        data.writeInt64(size);
        data.write(params, size);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(SET_CONFIG, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else   
        remote()->transact(SET_CONFIG, data, &reply);
#endif
        return reply.readInt32();
    }

    virtual status_t getState(
            node_id node, OMX_STATETYPE* state) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(GET_STATE, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else  
        remote()->transact(GET_STATE, data, &reply);
#endif
        *state = static_cast<OMX_STATETYPE>(reply.readInt32());
        return reply.readInt32();
    }

    virtual status_t enableGraphicBuffers(
            node_id node, OMX_U32 port_index, OMX_BOOL enable) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.writeInt32((uint32_t)enable);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(ENABLE_GRAPHIC_BUFFERS, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else  
        remote()->transact(ENABLE_GRAPHIC_BUFFERS, data, &reply);
#endif
        status_t err = reply.readInt32();
        return err;
    }

    virtual status_t getGraphicBufferUsage(
            node_id node, OMX_U32 port_index, OMX_U32* usage) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(GET_GRAPHIC_BUFFER_USAGE, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else    
        remote()->transact(GET_GRAPHIC_BUFFER_USAGE, data, &reply);
#endif
        status_t err = reply.readInt32();
        *usage = reply.readInt32();
        return err;
    }

    virtual status_t useBuffer(
            node_id node, OMX_U32 port_index, const sp<IMemory> &params,
            buffer_id *buffer, OMX_BOOL /* crossProcess */) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.writeStrongBinder(params->asBinder());
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(USE_BUFFER, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else   
        remote()->transact(USE_BUFFER, data, &reply);
#endif
        status_t err = reply.readInt32();
        if (err != OK) {
            *buffer = 0;

            return err;
        }

        *buffer = (buffer_id)reply.readInt32();

        return err;
    }


    virtual status_t useGraphicBuffer(
            node_id node, OMX_U32 port_index,
            const sp<GraphicBuffer> &graphicBuffer, buffer_id *buffer) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.write(*graphicBuffer);
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(USE_GRAPHIC_BUFFER, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else 
        remote()->transact(USE_GRAPHIC_BUFFER, data, &reply);
#endif
        status_t err = reply.readInt32();
        if (err != OK) {
            *buffer = 0;

            return err;
        }

        *buffer = (buffer_id)reply.readInt32();

        return err;
    }

    virtual status_t updateGraphicBufferInMeta(
            node_id node, OMX_U32 port_index,
            const sp<GraphicBuffer> &graphicBuffer, buffer_id buffer) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.write(*graphicBuffer);
        data.writeInt32((int32_t)buffer);
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(UPDATE_GRAPHIC_BUFFER_IN_META, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else  
        remote()->transact(UPDATE_GRAPHIC_BUFFER_IN_META, data, &reply);
#endif
        status_t err = reply.readInt32();
        return err;
    }

    virtual status_t createInputSurface(
            node_id node, OMX_U32 port_index,
            sp<IGraphicBufferProducer> *bufferProducer) {
        Parcel data, reply;
        status_t err;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        err = remote()->transact(CREATE_INPUT_SURFACE, data, &reply);
        if (err != OK) {
            ALOGW("binder transaction failed: %d", err);
            return err;
        }

        err = reply.readInt32();
        if (err != OK) {
            return err;
        }

        *bufferProducer = IGraphicBufferProducer::asInterface(
                reply.readStrongBinder());

        return err;
    }

    virtual status_t signalEndOfInputStream(node_id node) {
        Parcel data, reply;
        status_t err;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        err = remote()->transact(SIGNAL_END_OF_INPUT_STREAM, data, &reply);
        if (err != OK) {
            ALOGW("binder transaction failed: %d", err);
            return err;
        }

        return reply.readInt32();
    }

    virtual status_t storeMetaDataInBuffers(
            node_id node, OMX_U32 port_index, OMX_BOOL enable) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.writeInt32((uint32_t)enable);
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(STORE_META_DATA_IN_BUFFERS, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(STORE_META_DATA_IN_BUFFERS, data, &reply);
#endif
        status_t err = reply.readInt32();
        return err;
    }

    virtual status_t prepareForAdaptivePlayback(
            node_id node, OMX_U32 port_index, OMX_BOOL enable,
            OMX_U32 max_width, OMX_U32 max_height) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.writeInt32((int32_t)enable);
        data.writeInt32(max_width);
        data.writeInt32(max_height);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(PREPARE_FOR_ADAPTIVE_PLAYBACK, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(PREPARE_FOR_ADAPTIVE_PLAYBACK, data, &reply);
#endif
        status_t err = reply.readInt32();
        return err;
    }

    virtual status_t configureVideoTunnelMode(
            node_id node, OMX_U32 portIndex, OMX_BOOL tunneled,
            OMX_U32 audioHwSync, native_handle_t **sidebandHandle ) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(portIndex);
        data.writeInt32((int32_t)tunneled);
        data.writeInt32(audioHwSync);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(CONFIGURE_VIDEO_TUNNEL_MODE, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(CONFIGURE_VIDEO_TUNNEL_MODE, data, &reply);
#endif
        status_t err = reply.readInt32();
        if (err == OK && sidebandHandle) {
            *sidebandHandle = (native_handle_t *)reply.readNativeHandle();
        }
        return err;
    }


    virtual status_t allocateBuffer(
            node_id node, OMX_U32 port_index, size_t size,
            buffer_id *buffer, void **buffer_data) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.writeInt64(size);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(ALLOC_BUFFER, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(ALLOC_BUFFER, data, &reply);
#endif
        status_t err = reply.readInt32();
        if (err != OK) {
            *buffer = 0;

            return err;
        }

        *buffer = (buffer_id)reply.readInt32();
        *buffer_data = (void *)reply.readInt64();

        return err;
    }

    virtual status_t allocateBufferWithBackup(
            node_id node, OMX_U32 port_index, const sp<IMemory> &params,
            buffer_id *buffer, OMX_BOOL /* crossProcess */) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.writeStrongBinder(params->asBinder());
        
#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(ALLOC_BUFFER_WITH_BACKUP, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else
        remote()->transact(ALLOC_BUFFER_WITH_BACKUP, data, &reply);
#endif
        status_t err = reply.readInt32();
        if (err != OK) {
            *buffer = 0;

            return err;
        }

        *buffer = (buffer_id)reply.readInt32();

        return err;
    }

    virtual status_t freeBuffer(
            node_id node, OMX_U32 port_index, buffer_id buffer) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.writeInt32((int32_t)buffer);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(FREE_BUFFER, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else 
        remote()->transact(FREE_BUFFER, data, &reply);
#endif
        return reply.readInt32();
    }

    virtual status_t fillBuffer(node_id node, buffer_id buffer) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32((int32_t)buffer);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(FILL_BUFFER, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else 
        remote()->transact(FILL_BUFFER, data, &reply);
#endif
        return reply.readInt32();
    }

    virtual status_t emptyBuffer(
            node_id node,
            buffer_id buffer,
            OMX_U32 range_offset, OMX_U32 range_length,
            OMX_U32 flags, OMX_TICKS timestamp) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32((int32_t)buffer);
        data.writeInt32(range_offset);
        data.writeInt32(range_length);
        data.writeInt32(flags);
        data.writeInt64(timestamp);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(EMPTY_BUFFER, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else 
        remote()->transact(EMPTY_BUFFER, data, &reply);
#endif
        return reply.readInt32();
    }

    virtual status_t getExtensionIndex(
            node_id node,
            const char *parameter_name,
            OMX_INDEXTYPE *index) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeCString(parameter_name);


#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(GET_EXTENSION_INDEX, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else 
        remote()->transact(GET_EXTENSION_INDEX, data, &reply);
#endif

        status_t err = reply.readInt32();
        if (err == OK) {
            *index = static_cast<OMX_INDEXTYPE>(reply.readInt32());
        } else {
            *index = OMX_IndexComponentStartUnused;
        }

        return err;
    }

    virtual status_t setInternalOption(
            node_id node,
            OMX_U32 port_index,
            InternalOptionType type,
            const void *optionData,
            size_t size) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMX::getInterfaceDescriptor());
        data.writeInt32((int32_t)node);
        data.writeInt32(port_index);
        data.writeInt64(size);
        data.write(optionData, size);
        data.writeInt32(type);

#ifdef MTK_AOSP_ENHANCEMENT
        status_t status = remote()->transact(SET_INTERNAL_OPTION, data, &reply);
        if (status != NO_ERROR){
            ALOGE("binder transaction failed: %d", status);
            return status;
        }
#else 
        remote()->transact(SET_INTERNAL_OPTION, data, &reply);
#endif
        return reply.readInt32();
    }
};

IMPLEMENT_META_INTERFACE(OMX, "android.hardware.IOMX");

////////////////////////////////////////////////////////////////////////////////

#define CHECK_OMX_INTERFACE(interface, data, reply) \
        do { if (!data.enforceInterface(interface::getInterfaceDescriptor())) { \
            ALOGW("Call incorrectly routed to " #interface); \
            return PERMISSION_DENIED; \
        } } while (0)

status_t BnOMX::onTransact(
    uint32_t code, const Parcel &data, Parcel *reply, uint32_t flags) {
    switch (code) {
        case LIVES_LOCALLY:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);
            node_id node = (node_id)data.readInt32();
            pid_t pid = (pid_t)data.readInt32();
            reply->writeInt32(livesLocally(node, pid));

            return OK;
        }

        case LIST_NODES:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            List<ComponentInfo> list;
            listNodes(&list);

            reply->writeInt32(list.size());
            for (List<ComponentInfo>::iterator it = list.begin();
                 it != list.end(); ++it) {
                ComponentInfo &cur = *it;

                reply->writeString8(cur.mName);
                reply->writeInt32(cur.mRoles.size());
                for (List<String8>::iterator role_it = cur.mRoles.begin();
                     role_it != cur.mRoles.end(); ++role_it) {
                    reply->writeString8(*role_it);
                }
            }

            return NO_ERROR;
        }

        case ALLOCATE_NODE:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            const char *name = data.readCString();

            sp<IOMXObserver> observer =
                interface_cast<IOMXObserver>(data.readStrongBinder());

            node_id node;

            status_t err = allocateNode(name, observer, &node);
            reply->writeInt32(err);
            if (err == OK) {
                reply->writeInt32((int32_t)node);
            }

            return NO_ERROR;
        }

        case FREE_NODE:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();

            reply->writeInt32(freeNode(node));

            return NO_ERROR;
        }

        case SEND_COMMAND:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();

            OMX_COMMANDTYPE cmd =
                static_cast<OMX_COMMANDTYPE>(data.readInt32());

            OMX_S32 param = data.readInt32();
            reply->writeInt32(sendCommand(node, cmd, param));

            return NO_ERROR;
        }

        case GET_PARAMETER:
        case SET_PARAMETER:
        case GET_CONFIG:
        case SET_CONFIG:
        case SET_INTERNAL_OPTION:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_INDEXTYPE index = static_cast<OMX_INDEXTYPE>(data.readInt32());

            size_t size = data.readInt64();

            status_t err = NOT_ENOUGH_DATA;
            void *params = NULL;
            size_t pageSize = 0;
            size_t allocSize = 0;
            // this only applies to a subset of devices, and was obsoleted in M
            bool mightBeThumbnailMode = (code == SET_CONFIG && size == sizeof(OMX_BOOL));
            if (!mightBeThumbnailMode && (code != SET_INTERNAL_OPTION && size < 8)) {
                // we expect the structure to contain at least the size and
                // version, 8 bytes total
                ALOGE("b/27207275 (%zu)", size);
                //android_errorWriteLog(0x534e4554, "27207275");
            } else {
                err = NO_MEMORY;
                pageSize = (size_t) sysconf(_SC_PAGE_SIZE);
                if (size > SIZE_MAX - (pageSize * 2)) {
                    ALOGE("requested param size too big");
                } else {
                    allocSize = (size + pageSize * 2) & ~(pageSize - 1);
                    params = mmap(NULL, allocSize, PROT_READ | PROT_WRITE,
                            MAP_PRIVATE | MAP_ANONYMOUS, -1 /* fd */, 0 /* offset */);
                }
                if (params != MAP_FAILED) {
                err = data.read(params, size);
                if (err != OK) {
                    ALOGE("26914474");
                        //android_errorWriteLog(0x534e4554, "26914474");
                } else {
                        err = NOT_ENOUGH_DATA;
                        OMX_U32 declaredSize = *(OMX_U32*)params;
                        if (!mightBeThumbnailMode &&
                                code != SET_INTERNAL_OPTION && declaredSize > size) {
                            // the buffer says it's bigger than it actually is
                            ALOGE("b/27207275 (%u/%zu)", declaredSize, size);
                            //android_errorWriteLog(0x534e4554, "27207275");
                        } else {
                            // mark the last page as inaccessible, to avoid exploitation
                            // of codecs that access past the end of the allocation because
                            // they didn't check the size
                            if (mprotect((char*)params + allocSize - pageSize, pageSize,
                                    PROT_NONE) != 0) {
                                ALOGE("mprotect failed: %s", strerror(errno));
                            } else {
            switch (code) {
                case GET_PARAMETER:
                    err = getParameter(node, index, params, size);
                    break;
                case SET_PARAMETER:
                    err = setParameter(node, index, params, size);
                    break;
                case GET_CONFIG:
                    err = getConfig(node, index, params, size);
                    break;
                case SET_CONFIG:
                    err = setConfig(node, index, params, size);
                    break;
                case SET_INTERNAL_OPTION:
                {
                    InternalOptionType type =
                        (InternalOptionType)data.readInt32();

                    err = setInternalOption(node, index, type, params, size);
                    break;
                }

                default:
                    TRESPASS();
            }
                }
            }
                    }
                } else {
                    ALOGE("couldn't map: %s", strerror(errno));
                }
            }

            reply->writeInt32(err);

            if ((code == GET_PARAMETER || code == GET_CONFIG) && err == OK) {
                reply->write(params, size);
            }

            if (params) {
                munmap(params, allocSize);
            }
            params = NULL;

            return NO_ERROR;
        }

        case GET_STATE:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_STATETYPE state = OMX_StateInvalid;

            status_t err = getState(node, &state);
            reply->writeInt32(state);
            reply->writeInt32(err);

            return NO_ERROR;
        }

        case ENABLE_GRAPHIC_BUFFERS:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            OMX_BOOL enable = (OMX_BOOL)data.readInt32();

            status_t err = enableGraphicBuffers(node, port_index, enable);
            reply->writeInt32(err);

            return NO_ERROR;
        }

        case GET_GRAPHIC_BUFFER_USAGE:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();

            OMX_U32 usage = 0;
            status_t err = getGraphicBufferUsage(node, port_index, &usage);
            reply->writeInt32(err);
            reply->writeInt32(usage);

            return NO_ERROR;
        }

        case USE_BUFFER:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            sp<IMemory> params =
                interface_cast<IMemory>(data.readStrongBinder());

            buffer_id buffer;
            status_t err = useBuffer(
                    node, port_index, params, &buffer, OMX_TRUE /* crossProcess */);
            reply->writeInt32(err);

            if (err == OK) {
                reply->writeInt32((int32_t)buffer);
            }

            return NO_ERROR;
        }

        case USE_GRAPHIC_BUFFER:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            sp<GraphicBuffer> graphicBuffer = new GraphicBuffer();
            data.read(*graphicBuffer);

            buffer_id buffer;
            status_t err = useGraphicBuffer(
                    node, port_index, graphicBuffer, &buffer);
            reply->writeInt32(err);

            if (err == OK) {
                reply->writeInt32((int32_t)buffer);
            }

            return NO_ERROR;
        }

        case UPDATE_GRAPHIC_BUFFER_IN_META:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            sp<GraphicBuffer> graphicBuffer = new GraphicBuffer();
            data.read(*graphicBuffer);
            buffer_id buffer = (buffer_id)data.readInt32();

            status_t err = updateGraphicBufferInMeta(
                    node, port_index, graphicBuffer, buffer);
            reply->writeInt32(err);

            return NO_ERROR;
        }

        case CREATE_INPUT_SURFACE:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();

            sp<IGraphicBufferProducer> bufferProducer;
            status_t err = createInputSurface(node, port_index,
                    &bufferProducer);

            reply->writeInt32(err);

            if (err == OK) {
                reply->writeStrongBinder(bufferProducer->asBinder());
            }

            return NO_ERROR;
        }

        case SIGNAL_END_OF_INPUT_STREAM:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();

            status_t err = signalEndOfInputStream(node);
            reply->writeInt32(err);

            return NO_ERROR;
        }

        case STORE_META_DATA_IN_BUFFERS:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            OMX_BOOL enable = (OMX_BOOL)data.readInt32();

            status_t err =
                // only control output metadata via Binder
                port_index != 1 /* kOutputPortIndex */ ? BAD_VALUE :
                storeMetaDataInBuffers(node, port_index, enable);
            reply->writeInt32(err);

            return NO_ERROR;
        }

        case PREPARE_FOR_ADAPTIVE_PLAYBACK:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            OMX_BOOL enable = (OMX_BOOL)data.readInt32();
            OMX_U32 max_width = data.readInt32();
            OMX_U32 max_height = data.readInt32();

            status_t err = prepareForAdaptivePlayback(
                    node, port_index, enable, max_width, max_height);
            reply->writeInt32(err);

            return NO_ERROR;
        }

        case CONFIGURE_VIDEO_TUNNEL_MODE:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            OMX_BOOL tunneled = (OMX_BOOL)data.readInt32();
            OMX_U32 audio_hw_sync = data.readInt32();

            native_handle_t *sideband_handle = NULL;
            status_t err = configureVideoTunnelMode(
                    node, port_index, tunneled, audio_hw_sync, &sideband_handle);
            reply->writeInt32(err);
            if(err == OK){
                reply->writeNativeHandle(sideband_handle);
            }

            return NO_ERROR;
        }

        case ALLOC_BUFFER:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            if (!isSecure(node) || port_index != 0 /* kPortIndexInput */) {
                ALOGE("b/24310423");
                reply->writeInt32(INVALID_OPERATION);
                return NO_ERROR;
            }

            size_t size = data.readInt64();

            buffer_id buffer;
            void *buffer_data;
            status_t err = allocateBuffer(
                    node, port_index, size, &buffer, &buffer_data);
            reply->writeInt32(err);

            if (err == OK) {
                reply->writeInt32((int32_t)buffer);
                reply->writeInt64((uintptr_t)buffer_data);
            }

            return NO_ERROR;
        }

        case ALLOC_BUFFER_WITH_BACKUP:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            sp<IMemory> params =
                interface_cast<IMemory>(data.readStrongBinder());

            buffer_id buffer;
            status_t err = allocateBufferWithBackup(
                    node, port_index, params, &buffer, OMX_TRUE /* crossProcess */);

            reply->writeInt32(err);

            if (err == OK) {
                reply->writeInt32((int32_t)buffer);
            }

            return NO_ERROR;
        }

        case FREE_BUFFER:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            OMX_U32 port_index = data.readInt32();
            buffer_id buffer = (buffer_id)data.readInt32();
            reply->writeInt32(freeBuffer(node, port_index, buffer));

            return NO_ERROR;
        }

        case FILL_BUFFER:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            buffer_id buffer = (buffer_id)data.readInt32();
            reply->writeInt32(fillBuffer(node, buffer));

            return NO_ERROR;
        }

        case EMPTY_BUFFER:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            buffer_id buffer = (buffer_id)data.readInt32();
            OMX_U32 range_offset = data.readInt32();
            OMX_U32 range_length = data.readInt32();
            OMX_U32 flags = data.readInt32();
            OMX_TICKS timestamp = data.readInt64();

            reply->writeInt32(
                    emptyBuffer(
                        node, buffer, range_offset, range_length,
                        flags, timestamp));

            return NO_ERROR;
        }

        case GET_EXTENSION_INDEX:
        {
            CHECK_OMX_INTERFACE(IOMX, data, reply);

            node_id node = (node_id)data.readInt32();
            const char *parameter_name = data.readCString();

            OMX_INDEXTYPE index;
            status_t err = getExtensionIndex(node, parameter_name, &index);

            reply->writeInt32(err);

            if (err == OK) {
                reply->writeInt32(index);
            }

            return OK;
        }

        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

////////////////////////////////////////////////////////////////////////////////

class BpOMXObserver : public BpInterface<IOMXObserver> {
public:
    BpOMXObserver(const sp<IBinder> &impl)
        : BpInterface<IOMXObserver>(impl) {
    }

    virtual void onMessage(const omx_message &msg) {
        Parcel data, reply;
        data.writeInterfaceToken(IOMXObserver::getInterfaceDescriptor());
        data.write(&msg, sizeof(msg));

        ALOGV("onMessage writing message %d, size %zu", msg.type, sizeof(msg));

        remote()->transact(OBSERVER_ON_MSG, data, &reply, IBinder::FLAG_ONEWAY);
    }
};

IMPLEMENT_META_INTERFACE(OMXObserver, "android.hardware.IOMXObserver");

status_t BnOMXObserver::onTransact(
    uint32_t code, const Parcel &data, Parcel *reply, uint32_t flags) {
    switch (code) {
        case OBSERVER_ON_MSG:
        {
            CHECK_OMX_INTERFACE(IOMXObserver, data, reply);

            omx_message msg;
            data.read(&msg, sizeof(msg));

            ALOGV("onTransact reading message %d, size %zu", msg.type, sizeof(msg));

            // XXX Could use readInplace maybe?
            onMessage(msg);

            return NO_ERROR;
        }

        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

}  // namespace android
