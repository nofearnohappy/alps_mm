x RN 0 ; input array x[]
c RN 1 ; input array c[]
v RN 2 ; input array v[]
acc RN 4 ;
xx RN 5 ;
cc RN 6 ;
v_0 RN 7 ;
v_1 RN 12 ;
one RN 3 ;
cnt RN 8 ;

    MACRO
    PROCESS_TWO_SAMPLES
    LDR xx, [x], #4
    LDRSH v_0, [v], #2
    LDRSH v_1, [v], #2
    smlabt acc, xx, cc, acc
    LDR cc, [c], #4
    ADD v_1, v_1, xx, asr #16
    STRH v_1, [x, #-2]
    smlabb v_0, xx, one, v_0
    smlatb acc, xx, cc, acc
    STRH v_0, [x, #-4]    
    MEND
    
    AREA |s1.text|, CODE, READONLY
    EXPORT unaligned_dot_and_add_16
    ; int unaligned_dot_and_add_16(short *x, short *c, short *v)
unaligned_dot_and_add_16
    STMFD sp!, {r4-r7, lr}
    MOV acc, #0
    SUB c, c, #2
    LDR cc, [c], #4
    MOV one, #1

    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    
    MOV r0, acc
    LDMFD sp!, {r4-r7, pc}    
    
    AREA |s2.text|, CODE, READONLY
    EXPORT unaligned_dot_and_add_32
    ; int unaligned_dot_and_add_32(short *x, short *c, short *v)
unaligned_dot_and_add_32
    STMFD sp!, {r4-r7, lr}
    MOV acc, #0
    SUB c, c, #2
    LDR cc, [c], #4
    MOV one, #1

    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES

    MOV r0, acc
    LDMFD sp!, {r4-r7, pc}

    AREA |s3.text|, CODE, READONLY
    EXPORT unaligned_dot_and_add_64
    ; int unaligned_dot_and_add_64(short *x, short *c, short *v)
unaligned_dot_and_add_64
    STMFD sp!, {r4-r7, lr}
    MOV acc, #0
    SUB c, c, #2
    LDR cc, [c], #4
    MOV one, #1

    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES

    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES

    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES    

    MOV r0, acc
    LDMFD sp!, {r4-r7, pc}

    AREA |s4.text|, CODE, READONLY
    EXPORT unaligned_dot_and_add_256
    ; int unaligned_dot_and_add_256(short *x, short *c, short *v)
unaligned_dot_and_add_256
    STMFD sp!, {r4-r8, lr}
    MOV acc, #0
    SUB c, c, #2
    LDR cc, [c], #4
    MOV one, #1
    MOV cnt, #4

loop_256
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES

    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES

    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES    

    subs cnt, cnt, #1
    bne loop_256

    MOV r0, acc
    LDMFD sp!, {r4-r8, pc}

    AREA |s5.text|, CODE, READONLY
    EXPORT unaligned_dot_and_add_1280
    ; int unaligned_dot_and_add_1280(short *x, short *c, short *v)
unaligned_dot_and_add_1280
    STMFD sp!, {r4-r8, lr}
    MOV acc, #0
    SUB c, c, #2
    LDR cc, [c], #4
    MOV one, #1
    MOV cnt, #20

loop
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES

    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES

    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES
    PROCESS_TWO_SAMPLES    

    subs cnt, cnt, #1
    bne loop

    MOV r0, acc
    LDMFD sp!, {r4-r8, pc}
    END
