/*======================================================================*
 DTS, Inc.
 5220 Las Virgenes Road
 Calabasas, CA 91302  USA

 CONFIDENTIAL: CONTAINS CONFIDENTIAL PROPRIETARY INFORMATION OWNED BY
 DTS, INC. AND/OR ITS AFFILIATES ("DTS"), INCLUDING BUT NOT LIMITED TO
 TRADE SECRETS, KNOW-HOW, TECHNICAL AND BUSINESS INFORMATION. USE,
 DISCLOSURE OR DISTRIBUTION OF THE SOFTWARE IN ANY FORM IS LIMITED TO
 SPECIFICALLY AUTHORIZED LICENSEES OF DTS.  ANY UNAUTHORIZED
 DISCLOSURE IS A VIOLATION OF STATE, FEDERAL, AND INTERNATIONAL LAWS.
 BOTH CIVIL AND CRIMINAL PENALTIES APPLY.

 DO NOT DUPLICATE. COPYRIGHT 2014, DTS, INC. ALL RIGHTS RESERVED.
 UNAUTHORIZED DUPLICATION IS A VIOLATION OF STATE, FEDERAL AND
 INTERNATIONAL LAWS.

 ALGORITHMS, DATA STRUCTURES AND METHODS CONTAINED IN THIS SOFTWARE
 MAY BE PROTECTED BY ONE OR MORE PATENTS OR PATENT APPLICATIONS.
 UNLESS OTHERWISE PROVIDED UNDER THE TERMS OF A FULLY-EXECUTED WRITTEN
 AGREEMENT BY AND BETWEEN THE RECIPIENT HEREOF AND DTS, THE FOLLOWING
 TERMS SHALL APPLY TO ANY USE OF THE SOFTWARE (THE "PRODUCT") AND, AS
 APPLICABLE, ANY RELATED DOCUMENTATION:  (i) ANY USE OF THE PRODUCT
 AND ANY RELATED DOCUMENTATION IS AT THE RECIPIENT'S SOLE RISK:
 (ii) THE PRODUCT AND ANY RELATED DOCUMENTATION ARE PROVIDED "AS IS"
 AND WITHOUT WARRANTY OF ANY KIND AND DTS EXPRESSLY DISCLAIMS ALL
 WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 PURPOSE, REGARDLESS OF WHETHER DTS KNOWS OR HAS REASON TO KNOW OF THE
 USER'S PARTICULAR NEEDS; (iii) DTS DOES NOT WARRANT THAT THE PRODUCT
 OR ANY RELATED DOCUMENTATION WILL MEET USER'S REQUIREMENTS, OR THAT
 DEFECTS IN THE PRODUCT OR ANY RELATED DOCUMENTATION WILL BE
 CORRECTED; (iv) DTS DOES NOT WARRANT THAT THE OPERATION OF ANY
 HARDWARE OR SOFTWARE ASSOCIATED WITH THIS DOCUMENT WILL BE
 UNINTERRUPTED OR ERROR-FREE; AND (v) UNDER NO CIRCUMSTANCES,
 INCLUDING NEGLIGENCE, SHALL DTS OR THE DIRECTORS, OFFICERS, EMPLOYEES,
 OR AGENTS OF DTS, BE LIABLE TO USER FOR ANY INCIDENTAL, INDIRECT,
 SPECIAL, OR CONSEQUENTIAL DAMAGES (INCLUDING BUT NOT LIMITED TO
 DAMAGES FOR LOSS OF BUSINESS PROFITS, BUSINESS INTERRUPTION, AND LOSS
 OF BUSINESS INFORMATION) ARISING OUT OF THE USE, MISUSE, OR INABILITY
 TO USE THE PRODUCT OR ANY RELATED DOCUMENTATION.
*======================================================================*/

/********************************************************************************
 *	SRS Labs CONFIDENTIAL
 *	@Copyright 2010 by SRS Labs.
 *	All rights reserved.
 *
 *  Description:
 *  SRS ParametricEQ filter design types, constants
 *
 *  Authour: Oscar Huang
 *
 *  RCS keywords:
 *	$Id: //srstech/srs_designer/std_fxp/include/srs_parametriceq_design_def.h#1 $
 *  $Author: oscarh $
 *  $Date: 2010/09/26 $
 *	
********************************************************************************/
#ifndef __SRS_PARAMETRICEQ_DESIGN_DEF_H__
#define __SRS_PARAMETRICEQ_DESIGN_DEF_H__

#include "srs_filter_design_def.h"

typedef enum
{
	SRS_PEQ_BAND_TYPE_TRADITIONAL,
	SRS_PEQ_BAND_TYPE_LOWSHELF,
	SRS_PEQ_BAND_TYPE_HIGHSHELF,
	SRS_PEQ_BAND_TYPE_LOWPASS,
	SRS_PEQ_BAND_TYPE_HIGHPASS,
	SRS_PEQ_BAND_TYPE_BANDPASS,
	SRS_PEQ_BAND_TYPE_ALLPASS,
	SRS_PEQ_BAND_TYPE_NOTCH,
	SRS_PEQ_BAND_TYPE_PEAKING,
	SRS_PEQ_BAND_TYPE_NULL,
	SRS_PEQ_BAND_TYPE_NUM
} SRSParametriceqBandType;

typedef struct
{
	SRSParametriceqBandType		Type;
	float						CenterFreq;		//in Hz, physical frequency
	float						QFactor;		//ratio of band width/CenterFreq
	float						Gain;			//gain of the band in dB
	int							InvertPhase;	//Invert phase if nonzero
} SRSParametriceqBandSpec;


typedef struct
{
	int						NumOfBands;		//The number of bands
	SRSParametriceqBandSpec *BandSpecs;		//Specification array of all bands
	int						SampleRate;	//Sampling rate in Hz
} SRSParametriceqSpec;

#define SRS_PEQ_FLOAT_COEFFICIENT_ARRAY_LEN(nBands)		(5*(nBands)+1)	//in float type elements

#define SRS_PEQ_FXP_COEFFICIENT_ARRAY_LEN(nBands)		(6*(nBands)+2)	//in SRSInt32 type elements

#define SRS_PEQ_DESIGN_WORKSPACE_SIZE(nBands)	(((nBands)*5+1)*sizeof(float)+(5*(nBands)+3*ANALYSIS_BLOCKSIZE)*sizeof(float)+8) //in bytes


#endif //__SRS_PARAMETRICEQ_DESIGN_DEF_H__

