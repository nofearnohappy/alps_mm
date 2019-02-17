/*
 * CMetaNfc.cpp
 *
 *  Created on: 2011-7-28
 *      Author: mtk80905
 */

#define MTK_LOG_ENABLE 1
#include "CMetaNfc.h"
#include "XSync.h"
#include "meta_nfc_para.h"
#include "comDef.h"
#include <string.h>

#define LOG_TAG "nfc"
#include <cutils/log.h>

struct SyncedElement {
public:
	SyncedElement() :
		resp(0), food_respOK(0), lock(0), isReqSent(0) {
		food_respOK = new XSemphore();
		food_respOK->Init(0);
		ALOGD("sam valid %d", food_respOK->IsValid());
		lock = new XLock();
	}
	virtual ~SyncedElement() {
		if (food_respOK) {
			delete food_respOK;
			food_respOK = 0;
		}
		if (lock) {
			delete lock;
			lock = 0;
		}
	}

	bool TimedWait(int delay) {
        if (food_respOK == NULL) {
            return false;
        }
		if (0 != food_respOK->TimedWait(delay)) {
			return false;
		} else {
			return true;
		}
	}

	void NotifyWait() {
		food_respOK->Post();
	}

	void TransferLock() {
		lock->Lock();
	}
	void TransferUnLock() {
		lock->Unlock();
	}

	friend class CMetaNfc;
private:
	NFC_CNF* resp;
	XSemphore* food_respOK;
	//caution: the same op only support 1 thread.
	XLock* lock;

	bool isReqSent;  //prevent multi resp with 1 req.

};

SyncedElement** CMetaNfc::respMap = 0;
XLock* CMetaNfc::mNewObjectLock = 0;

void CMetaNfc::Init() {
	if(respMap == 0)
	{
		respMap = new SyncedElement*[NFC_OP_END];
		memset((void*) respMap, 0, NFC_OP_END * sizeof(SyncedElement*));
	}
	if(mNewObjectLock == 0)
	{
		mNewObjectLock = new XLock();
	}
	ALOGD("CMetaNfc Init");
	return;
}

void CMetaNfc::DeInit() {
	if (respMap != 0) {
		for (int i = 0; i < NFC_OP_END; i++) {
			if (respMap[i] != 0) {
				delete respMap[i];
				respMap[i] = 0;
			}
		}
		delete respMap;
		respMap = 0;
	}

	if (mNewObjectLock != 0) {
		delete mNewObjectLock;
		mNewObjectLock = 0;
	}
	ALOGD("CMetaNfc DeInit");
	return;
}
int CMetaNfc::getOPIdx(int op)
{
	return op;
}

int CMetaNfc::SendCommand(NFC_REQ *req, char *peer_buff,
		unsigned short peer_len, /*IN OUT*/NFC_CNF* resp) {

	ALOGD("CMetaNfc::SendCommand, CMD=%d, NFC_OP_END=%d", req->op, NFC_OP_END);
	int idx = getOPIdx(req->op);

	mNewObjectLock->Lock();
	ALOGD("CMetaNfc::SendCommand  locked");
	if (respMap[idx] == 0) {
		ALOGD("CMetaNfc::SendCommand  new SyncedElement");
		respMap[idx] = new SyncedElement();
	}
	mNewObjectLock->Unlock();
	ALOGD("CMetaNfc::SendCommand  unlocked");

	//respMap[idx]->TransferLock();
	ALOGD("CMetaNfc::SendCommand  TransferLocked");

	respMap[idx]->isReqSent = true;
	META_NFC_OP(req, peer_buff, peer_len);
	ALOGD("META_NFC_OP()  returned");
    
    if (respMap == NULL || respMap[idx] == NULL) {
        ALOGD("respMap == NULL");
        return ERR_OK;           
    }
        
	if (!respMap[idx]->TimedWait(50000))//wait for 50000sec
	{
		//timeout
		ALOGE("META_NFC_OP 50000 Seconds TimeOut.");
		resp->status = RESULT_STATUS_TIMEOUT;
		respMap[idx]->isReqSent = false;
		respMap[idx]->TransferUnLock();
		return ERR_TIMEOUT;
	} else {   
	
        if (respMap == NULL || respMap[idx] == NULL || respMap[idx]->resp == NULL) {
            ALOGD("respMap == NULL");
            return ERR_OK; 
        }	
    
        memcpy(resp, respMap[idx]->resp, sizeof(NFC_CNF));
	}
	ALOGD("META_NFC_OP()  Sync returned");

	respMap[idx]->isReqSent = false;
	//respMap[idx]->TransferUnLock();
	ALOGD("CMetaNfc::SendCommand  Transfer UnLocked");
	return ERR_OK;
}

void CMetaNfc::NotifyResponse(NFC_CNF* resp) {
	int idx = getOPIdx(resp->op);

	ALOGE("NotifyResponse getOPIdx %d", idx);
	if (respMap[idx] != 0) {
		if(!respMap[idx]->isReqSent)
		{
			ALOGE("Protocol Error: multi-response.");
			//return;
		}
		respMap[idx]->resp = resp;
		respMap[idx]->NotifyWait();
		ALOGD("Response returned, NotifyResponse().");
	} else {
		ALOGE("PROTOCOL ERROR, NotifyResponse respMap[idx] == 0");
	}
}

