/*
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package mockit.external.asm;

import static mockit.external.asm.ClassWriter.*;
import static mockit.external.asm.ClassWriter.ConstantPoolItemType.DOUBLE;
import static mockit.external.asm.ClassWriter.ConstantPoolItemType.LONG;
import static mockit.external.asm.Opcodes.*;

/**
 * A {@link MethodVisitor} that generates methods in bytecode form. Each visit method of this class appends the bytecode
 * corresponding to the visited instruction to a byte vector, in the order these methods are called.
 *
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
public final class MethodWriter extends MethodVisitor
{
   /**
    * Pseudo access flag used to denote constructors.
    */
   static final int ACC_CONSTRUCTOR = 0x80000;

   /**
    * Frame has exactly the same locals as the previous stack map frame and number of stack items is zero.
    */
   static final int SAME_FRAME = 0; // to 63 (0-3f)

   /**
    * Frame has exactly the same locals as the previous stack map frame and number of stack items is 1.
    */
   static final int SAME_LOCALS_1_STACK_ITEM_FRAME = 64; // to 127 (40-7f)

   /**
    * Frame has exactly the same locals as the previous stack map frame and number of stack items is 1.
    * Offset is bigger then 63.
    */
   static final int SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247; // f7

   /**
    * Frame where current locals are the same as the locals in the previous frame, except that the k last locals are
    * absent. The value of k is given by the formula 251-frame_type.
    */
   static final int CHOP_FRAME = 248; // to 250 (f8-fA)

   /**
    * Frame has exactly the same locals as the previous stack map frame and number of stack items is zero.
    * Offset is bigger then 63.
    */
   static final int SAME_FRAME_EXTENDED = 251; // fb

   /**
    * Frame where current locals are the same as the locals in the previous frame, except that k additional locals are
    * defined. The value of k is given by the formula frame_type-251.
    */
   static final int APPEND_FRAME = 252; // to 254 // fc-fe

   /**
    * Full frame.
    */
   static final int FULL_FRAME = 255; // ff

   /**
    * The class writer to which this method must be added.
    */
   final ClassWriter cw;

   /**
    * Access flags of this method.
    */
   final int access;

   /**
    * The index of the constant pool item that contains the name of this method.
    */
   private final int name;

   /**
    * The index of the constant pool item that contains the descriptor of this method.
    */
   private final int desc;

   /**
    * The descriptor of this method.
    */
   final String descriptor;

   /**
    * The signature of this method.
    */
   String signature;

   /**
    * If not zero, indicates that the code of this method must be copied from the ClassReader associated to this writer
    * in <code>cw.cr</code>. More precisely, this field gives the index of the first byte to copied from
    * <code>cw.cr.b</code>.
    */
   int classReaderOffset;

   /**
    * If not zero, indicates that the code of this method must be copied from the ClassReader associated to this writer
    * in <code>cw.cr</code>. More precisely, this field gives the number of bytes to copied from <code>cw.cr.b</code>.
    */
   int classReaderLength;

   /**
    * Number of exceptions that can be thrown by this method.
    */
   final int exceptionCount;

   /**
    * The exceptions that can be thrown by this method. More precisely, this array contains the indexes of the constant
    * pool items that contain the internal names of these exception classes.
    */
   final int[] exceptions;

   /**
    * The annotation default attribute of this method. May be <tt>null</tt>.
    */
   private ByteVector annotationDefault;

   /**
    * The runtime visible parameter annotations of this method. May be <tt>null</tt>.
    */
   private AnnotationWriter[] parameterAnnotations;

   /**
    * The bytecode of this method.
    */
   final ByteVector code;

   private final FrameAndStackComputation frameAndStack;

   /**
    * Number of elements in the exception handler list.
    */
   private int handlerCount;

   /**
    * The first element in the exception handler list.
    */
   private ExceptionHandler firstExceptionHandler;

   /**
    * The last element in the exception handler list.
    */
   private ExceptionHandler lastExceptionHandler;

   /**
    * Number of entries in the LocalVariableTable attribute.
    */
   private int localVarCount;

   /**
    * The LocalVariableTable attribute.
    */
   private ByteVector localVar;

   /**
    * Number of entries in the LocalVariableTypeTable attribute.
    */
   private int localVarTypeCount;

   /**
    * The LocalVariableTypeTable attribute.
    */
   private ByteVector localVarType;

   /**
    * Number of entries in the LineNumberTable attribute.
    */
   private int lineNumberCount;

   /**
    * The LineNumberTable attribute.
    */
   private ByteVector lineNumber;

   /**
    * The number of subroutines in this method.
    */
   private int subroutines;

   // Fields for the control flow graph analysis algorithm (used to compute the maximum stack size). A control flow
   // graph contains one node per "basic block", and one edge per "jump" from one basic block to another. Each node
   // (i.e., each basic block) is represented by the Label object that corresponds to the first instruction of this
   // basic block. Each node also stores the list of its successors in the graph, as a linked list of Edge objects.
   //

   /**
    * Indicates whether frames AND max stack/locals must be automatically computed, or if only max stack/locals must be.
    */
   private final boolean computeFrames;

   /**
    * A list of labels. This list is the list of basic blocks in the method, i.e. a list of Label objects linked to each
    * other by their {@link Label#successor} field, in the order they are visited by {@link MethodVisitor#visitLabel},
    * and starting with the first basic block.
    */
   private final Label labels;

   /**
    * The previous basic block.
    */
   private Label previousBlock;

   /**
    * The current basic block.
    */
   public Label currentBlock;

   /**
    * The (relative) stack size after the last visited instruction. This size is relative to the beginning of the
    * current basic block, i.e., the true stack size after the last visited instruction is equal to the
    * {@link Label#inputStackTop beginStackSize} of the current basic block plus <tt>stackSize</tt>.
    */
   public int stackSize;

   /**
    * The (relative) maximum stack size after the last visited instruction. This size is relative to the beginning of
    * the current basic block, i.e., the true maximum stack size after the last visited instruction is equal to the
    * {@link Label#inputStackTop beginStackSize} of the current basic block plus <tt>stackSize</tt>.
    */
   private int maxStackSize;

   /**
    * Constructs a new {@link MethodWriter}.
    *
    * @param cw            the class writer in which the method must be added.
    * @param access        the method's access flags (see {@link Opcodes}).
    * @param name          the method's name.
    * @param desc          the method's descriptor (see {@link Type}).
    * @param signature     the method's signature. May be <tt>null</tt>.
    * @param exceptions    the internal names of the method's exceptions. May be <tt>null</tt>.
    * @param computeFrames {@code true} if the stack map tables must be recomputed from scratch.
    */
   MethodWriter(
      ClassWriter cw, int access, String name, String desc, String signature, String[] exceptions, boolean computeFrames
   ) {
      this.cw = cw;
      this.access = "<init>".equals(name) ? (access | ACC_CONSTRUCTOR) : access;
      this.name = cw.newUTF8(name);
      this.desc = cw.newUTF8(desc);
      descriptor = desc;
      this.signature = signature;
      exceptionCount = exceptions == null ? 0 : exceptions.length;
      this.exceptions = initializeExceptions(exceptions);
      code = new ByteVector();

      this.computeFrames = computeFrames;
      frameAndStack = new FrameAndStackComputation(this, access, desc);

      labels = createAndVisitLabelForFirstBasicBlock();
   }

   private int[] initializeExceptions(String[] exceptions) {
      int n = exceptionCount;

      if (n > 0) {
         int[] exceptionIndices = new int[n];

         for (int i = 0; i < n; ++i) {
            exceptionIndices[i] = cw.newClass(exceptions[i]);
         }

         return exceptionIndices;
      }

      return null;
   }

   private Label createAndVisitLabelForFirstBasicBlock() {
      Label label = new Label();
      label.status |= Label.PUSHED;
      visitLabel(label);
      return label;
   }

   // ------------------------------------------------------------------------
   // Implementation of the MethodVisitor base class
   // ------------------------------------------------------------------------

   @Override
   public AnnotationVisitor visitAnnotationDefault() {
      annotationDefault = new ByteVector();
      return new AnnotationWriter(cw, false, annotationDefault, null, 0);
   }

   @Override
   public AnnotationVisitor visitAnnotation(String desc) {
      return visitAnnotation(cw, desc);
   }

   @Override
   public AnnotationVisitor visitParameterAnnotation(int parameter, String desc) {
      ByteVector bv = new ByteVector();

      // Write type, and reserve space for values count.
      bv.putShort(cw.newUTF8(desc)).putShort(0);

      AnnotationWriter aw = new AnnotationWriter(cw, true, bv, bv, 2);

      if (parameterAnnotations == null) {
         int numParameters = Type.getArgumentTypes(descriptor).length;
         parameterAnnotations = new AnnotationWriter[numParameters];
      }

      aw.next = parameterAnnotations[parameter];
      parameterAnnotations[parameter] = aw;

      return aw;
   }

   @Override
   public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
      if (computeFrames) {
         return;
      }

      frameAndStack.readFrame(type, nLocal, local, nStack, stack);
   }

   @Override
   public void visitInsn(int opcode) {
      // Adds the instruction to the bytecode of the method.
      code.putByte(opcode);

      // Update currentBlock.
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, 0, null, null);
         }
         else {
            // Updates current and max stack sizes.
            int size = stackSize + Frame.SIZE[opcode];

            if (size > maxStackSize) {
               maxStackSize = size;
            }

            stackSize = size;
         }

         // If opcode == ATHROW or xRETURN, ends current block (no successor).
         if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
            noSuccessor();
         }
      }
   }

   @Override
   public void visitIntInsn(int opcode, int operand) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, operand, null, null);
         }
         else if (opcode != NEWARRAY) {
            // Updates current and max stack sizes only for NEWARRAY (stack size variation = 0 for BIPUSH or SIPUSH).
            int size = stackSize + 1;

            if (size > maxStackSize) {
               maxStackSize = size;
            }

            stackSize = size;
         }
      }

      // Adds the instruction to the bytecode of the method.
      if (opcode == SIPUSH) {
         code.put12(opcode, operand);
      }
      else { // BIPUSH or NEWARRAY
         code.put11(opcode, operand);
      }
   }

   @Override
   public void visitVarInsn(int opcode, int var) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, var, null, null);
         }
         else {
            // Updates current and max stack sizes.
            if (opcode == RET) {
               // No stack change, but end of current block (no successor).
               currentBlock.status |= Label.RET;

               // Save 'stackSize' here for future use (see {@link #findSubroutineSuccessors}).
               currentBlock.inputStackTop = stackSize;
               noSuccessor();
            }
            else { // xLOAD or xSTORE
               int size = stackSize + Frame.SIZE[opcode];

               if (size > maxStackSize) {
                  maxStackSize = size;
               }

               stackSize = size;
            }
         }
      }

      // Updates max locals.
      int n = opcode == LLOAD || opcode == DLOAD || opcode == LSTORE || opcode == DSTORE ? var + 2 : var + 1;
      frameAndStack.updateMaxLocals(n);

      // Adds the instruction to the bytecode of the method.
      if (var < 4 && opcode != RET) {
         int opt;

         if (opcode < ISTORE) { // ILOAD_0
            opt = 26 + ((opcode - ILOAD) << 2) + var;
         }
         else { // ISTORE_0
            opt = 59 + ((opcode - ISTORE) << 2) + var;
         }

         code.putByte(opt);
      }
      else if (var >= 256) {
         code.putByte(196 /* WIDE */).put12(opcode, var);
      }
      else {
         code.put11(opcode, var);
      }

      if (opcode >= ISTORE && computeFrames && handlerCount > 0) {
         visitLabel(new Label());
      }
   }

   @Override
   public void visitTypeInsn(int opcode, String type) {
      Item i = cw.newClassItem(type);

      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, code.length, cw, i);
         }
         else if (opcode == NEW) {
            // Updates current and max stack sizes only if opcode == NEW
            // (no stack change for ANEWARRAY, CHECKCAST, INSTANCEOF).
            int size = stackSize + 1;

            if (size > maxStackSize) {
               maxStackSize = size;
            }

            stackSize = size;
         }
      }

      // Adds the instruction to the bytecode of the method.
      code.put12(opcode, i.index);
   }

   @Override
   public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      Item i = cw.newFieldItem(owner, name, desc);

      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, 0, cw, i);
         }
         else {
            char c = desc.charAt(0);
            int size;

            // Computes the stack size variation.
            switch (opcode) {
               case GETSTATIC:
                  size = stackSize + (c == 'D' || c == 'J' ? 2 : 1);
                  break;
               case PUTSTATIC:
                  size = stackSize + (c == 'D' || c == 'J' ? -2 : -1);
                  break;
               case GETFIELD:
                  size = stackSize + (c == 'D' || c == 'J' ? 1 : 0);
                  break;
               // case Constants.PUTFIELD:
               default:
                  size = stackSize + (c == 'D' || c == 'J' ? -3 : -2);
                  break;
            }

            // Updates current and max stack sizes.
            if (size > maxStackSize) {
               maxStackSize = size;
            }

            stackSize = size;
         }
      }

      // Adds the instruction to the bytecode of the method.
      code.put12(opcode, i.index);
   }

   @Override
   public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      Item i = cw.newMethodItem(owner, name, desc, itf);
      int argSize = i.intVal;

      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, 0, cw, i);
         }
         else {
            /*
             * Computes the stack size variation. In order not to recompute several times this variation for the same
             * Item, we use the intVal field of this item to store this variation, once it has been computed. More
             * precisely this intVal field stores the sizes of the arguments and of the return value corresponding to
             * desc.
             */
            if (argSize == 0) {
               // The above sizes have not been computed yet, so we compute them...
               argSize = Type.getArgumentsAndReturnSizes(desc);

               // ... and we save them in order not to recompute them in the future.
               i.intVal = argSize;
            }

            int size = stackSize - (argSize >> 2) + (argSize & 0x03);

            if (opcode == INVOKESTATIC) {
               size++;
            }

            // Updates current and max stack sizes.
            if (size > maxStackSize) {
               maxStackSize = size;
            }

            stackSize = size;
         }
      }

      // Adds the instruction to the bytecode of the method.
      if (opcode == INVOKEINTERFACE) {
         if (argSize == 0) {
            argSize = Type.getArgumentsAndReturnSizes(desc);
            i.intVal = argSize;
         }

         code.put12(INVOKEINTERFACE, i.index).put11(argSize >> 2, 0);
      }
      else {
         code.put12(opcode, i.index);
      }
   }

   @Override
   public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
      Item i = cw.newInvokeDynamicItem(name, desc, bsm, bsmArgs);
      int argSize = i.intVal;

      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(INVOKEDYNAMIC, 0, cw, i);
         }
         else {
            /*
             * Computes the stack size variation. In order not to recompute several times this variation for the same
             * Item, we use the intVal field of this item to store this variation, once it has been computed. More
             * precisely this intVal field stores the sizes of the arguments and of the return value corresponding to
             * desc.
             */
            if (argSize == 0) {
               // The above sizes have not been computed yet, so we compute them...
               argSize = Type.getArgumentsAndReturnSizes(desc);

               // ... and we save them in order not to recompute them in the future.
               i.intVal = argSize;
            }

            int size = stackSize - (argSize >> 2) + (argSize & 0x03) + 1;

            // Updates current and max stack sizes.
            if (size > maxStackSize) {
               maxStackSize = size;
            }

            stackSize = size;
         }
      }

      // Adds the instruction to the bytecode of the method.
      code.put12(INVOKEDYNAMIC, i.index);
      code.putShort(0);
   }

   @Override
   public void visitJumpInsn(int opcode, Label label) {
      Label nextInsn = null;

      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(opcode, 0, null, null);

            // 'label' is the target of a jump instruction.
            label.getFirst().setAsTarget();

            // Adds 'label' as a successor of this basic block.
            addSuccessor(Edge.NORMAL, label);

            if (opcode != GOTO) {
               // Creates a Label for the next basic block.
               nextInsn = new Label();
            }
         }
         else {
            if (opcode == JSR) {
               if ((label.status & Label.SUBROUTINE) == 0) {
                  label.status |= Label.SUBROUTINE;
                  ++subroutines;
               }

               currentBlock.setAsJSR();
               addSuccessor(stackSize + 1, label);

               // Creates a Label for the next basic block.
               nextInsn = new Label();
               /*
                * Note that, by construction in this method, a JSR block has at least two successors in the control flow
                * graph: the first one leads the next instruction after the JSR, while the second one leads to the JSR
                * target.
                */
            }
            else {
               // Updates current stack size (max stack size unchanged because stack size variation always negative in
               // this case).
               stackSize += Frame.SIZE[opcode];
               addSuccessor(stackSize, label);
            }
         }
      }

      // Adds the instruction to the bytecode of the method.
      if ((label.status & Label.RESOLVED) != 0 && label.position - code.length < Short.MIN_VALUE) {
         /*
          * Case of a backward jump with an offset < -32768. In this case we automatically replace GOTO with GOTO_W,
          * JSR with JSR_W and IFxxx <l> with IFNOTxxx <l'> GOTO_W <l>, where IFNOTxxx is the "opposite" opcode of IFxxx
          * (i.e., IFNE for IFEQ) and where <l'> designates the instruction just after the GOTO_W.
          */
         if (opcode == GOTO) {
            code.putByte(200); // GOTO_W
         }
         else if (opcode == JSR) {
            code.putByte(201); // JSR_W
         }
         else {
            // If the IF instruction is transformed into IFNOT GOTO_W the next instruction becomes the target of the
            // IFNOT instruction.
            if (nextInsn != null) {
               nextInsn.setAsTarget();
            }

            code.putByte(opcode <= 166 ? ((opcode + 1) ^ 1) - 1 : opcode ^ 1);
            code.putShort(8); // jump offset
            code.putByte(200); // GOTO_W
         }

         label.put(code, code.length - 1, true);
      }
      else {
         /*
          * Case of a backward jump with an offset >= -32768, or of a forward jump with, of course, an unknown offset.
          * In these cases we store the offset in 2 bytes (which will be increased in resizeInstructions, if needed).
          */
         code.putByte(opcode);
         label.put(code, code.length - 1, false);
      }

      if (currentBlock != null) {
         if (nextInsn != null) {
            // If the jump instruction is not a GOTO, the next instruction is also a successor of this instruction.
            // Calling visitLabel adds the label of this next instruction as a successor of the current block, and
            // starts a new basic block.
            visitLabel(nextInsn);
         }

         if (opcode == GOTO) {
            noSuccessor();
         }
      }
   }

   @Override
   public void visitLabel(Label label) {
      // Resolves previous forward references to label, if any.
      label.resolve(code);

      // Updates currentBlock.
      if (label.isDebug()) {
         return;
      }

      if (computeFrames) {
         if (currentBlock != null) {
            if (label.position == currentBlock.position) {
               // Successive labels, do not start a new basic block.
               currentBlock.status |= label.status & Label.TARGET;
               label.frame = currentBlock.frame;
               return;
            }

            // Ends current block (with one new successor).
            addSuccessor(Edge.NORMAL, label);
         }

         // Begins a new current block.
         currentBlock = label;

         if (label.frame == null) {
            label.frame = new Frame(label);
         }

         // Updates the basic block list.
         if (previousBlock != null) {
            if (label.position == previousBlock.position) {
               previousBlock.status |= label.status & Label.TARGET;
               label.frame = previousBlock.frame;
               currentBlock = previousBlock;
               return;
            }

            previousBlock.successor = label;
         }

         previousBlock = label;
      }
      else {
         if (currentBlock != null) {
            // Ends current block (with one new successor).
            currentBlock.outputStackMax = maxStackSize;
            addSuccessor(stackSize, label);
         }

         // Begins a new current block
         currentBlock = label;

         // Resets the relative current and max stack sizes.
         stackSize = 0;
         maxStackSize = 0;

         // Updates the basic block list.
         if (previousBlock != null) {
            previousBlock.successor = label;
         }

         previousBlock = label;
      }
   }

   @Override
   public void visitLdcInsn(Object cst) {
      Item i = cw.newConstItem(cst);

      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(LDC, 0, cw, i);
         }
         else {
            int size;

            // Computes the stack size variation.
            if (i.type == LONG || i.type == DOUBLE) {
               size = stackSize + 2;
            }
            else {
               size = stackSize + 1;
            }

            // Updates current and max stack sizes.
            if (size > maxStackSize) {
               maxStackSize = size;
            }

            stackSize = size;
         }
      }

      // Adds the instruction to the bytecode of the method.
      int index = i.index;

      if (i.type == LONG || i.type == DOUBLE) {
         code.put12(20 /* LDC2_W */, index);
      }
      else if (index >= 256) {
         code.put12(19 /* LDC_W */, index);
      }
      else {
         code.put11(LDC, index);
      }
   }

   @Override
   public void visitIincInsn(int var, int increment) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(IINC, var, null, null);
         }
      }

      // Updates max locals.
      int n = var + 1;
      frameAndStack.updateMaxLocals(n);

      // Adds the instruction to the bytecode of the method.
      if (var > 255 || increment > 127 || increment < -128) {
         code.putByte(196 /* WIDE */).put12(IINC, var).putShort(increment);
      }
      else {
         code.putByte(IINC).put11(var, increment);
      }
   }

   @Override
   public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      // Adds the instruction to the bytecode of the method.
      int source = code.length;
      code.putByte(TABLESWITCH);
      code.increaseLengthBy((4 - code.length % 4) % 4);
      dflt.put(code, source, true);
      code.putInt(min).putInt(max);

      for (int i = 0; i < labels.length; ++i) {
         labels[i].put(code, source, true);
      }

      // Updates currentBlock.
      visitSwitchInsn(dflt, labels);
   }

   @Override
   public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      // Adds the instruction to the bytecode of the method.
      int source = code.length;
      code.putByte(LOOKUPSWITCH);
      code.increaseLengthBy((4 - code.length % 4) % 4);
      dflt.put(code, source, true);
      code.putInt(labels.length);

      for (int i = 0; i < labels.length; ++i) {
         code.putInt(keys[i]);
         labels[i].put(code, source, true);
      }

      // Updates currentBlock.
      visitSwitchInsn(dflt, labels);
   }

   private void visitSwitchInsn(Label dflt, Label[] labels) {
      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(LOOKUPSWITCH, 0, null, null);
            // Adds current block successors.
            addSuccessor(Edge.NORMAL, dflt);
            dflt.getFirst().setAsTarget();

            for (int i = 0; i < labels.length; ++i) {
               addSuccessor(Edge.NORMAL, labels[i]);
               labels[i].getFirst().setAsTarget();
            }
         }
         else {
            // Updates current stack size (max stack size unchanged).
            --stackSize;

            // Adds current block successors.
            addSuccessor(stackSize, dflt);

            for (int i = 0; i < labels.length; ++i) {
               addSuccessor(stackSize, labels[i]);
            }
         }

         // Ends current block.
         noSuccessor();
      }
   }

   @Override
   public void visitMultiANewArrayInsn(String desc, int dims) {
      Item i = cw.newClassItem(desc);

      if (currentBlock != null) {
         if (computeFrames) {
            currentBlock.frame.execute(MULTIANEWARRAY, dims, cw, i);
         }
         else {
            // Updates current stack size (max stack size unchanged because stack size variation always negative or
            // null).
            stackSize += 1 - dims;
         }
      }

      // Adds the instruction to the bytecode of the method.
      code.put12(MULTIANEWARRAY, i.index).putByte(dims);
   }

   @Override
   public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
      ++handlerCount;

      int handlerType = type != null ? cw.newClass(type) : 0;
      ExceptionHandler h = new ExceptionHandler(start, end, handler, type, handlerType);

      if (lastExceptionHandler == null) {
         firstExceptionHandler = h;
      }
      else {
         lastExceptionHandler.next = h;
      }

      lastExceptionHandler = h;
   }

   @Override
   public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      if (signature != null) {
         if (localVarType == null) {
            localVarType = new ByteVector();
         }

         ++localVarTypeCount;
         localVarType.putShort(start.position)
            .putShort(end.position - start.position)
            .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(signature))
            .putShort(index);
      }

      if (localVar == null) {
         localVar = new ByteVector();
      }

      ++localVarCount;
      localVar.putShort(start.position)
         .putShort(end.position - start.position)
         .putShort(cw.newUTF8(name)).putShort(cw.newUTF8(desc))
         .putShort(index);

      // Updates max locals.
      char c = desc.charAt(0);
      int n = index + (c == 'J' || c == 'D' ? 2 : 1);
      frameAndStack.updateMaxLocals(n);
   }

   @Override
   public void visitLineNumber(int line, Label start) {
      if (lineNumber == null) {
         lineNumber = new ByteVector();
      }

      ++lineNumberCount;
      lineNumber.putShort(start.position);
      lineNumber.putShort(line);
   }

   @Override
   public void visitMaxStack(int maxStack) {
      if (computeFrames) {
         completeControlFlowGraphWithExceptionHandlerBlocksFromComputedFrames();
         createAndVisitFirstFrame();

         int max = computeMaxStackSizeFromComputedFrames();
         max = visitAllFramesToBeStoredInStackMap(max);

         countNumberOfHandlers();
         frameAndStack.maxStack = max;
      }
      else {
         completeControlFlowGraphWithExceptionHandlerBlocks();

         if (subroutines > 0) {
            completeControlFlowGraphWithRETSuccessors();
         }

         int computedMaxStack = computeMaxStackSize();
         frameAndStack.maxStack = Math.max(maxStack, computedMaxStack);
      }
   }

   private void completeControlFlowGraphWithExceptionHandlerBlocksFromComputedFrames() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;

      while (exceptionHandler != null) {
         Label l = exceptionHandler.start.getFirst();
         Label h = exceptionHandler.handler.getFirst();
         Label e = exceptionHandler.end.getFirst();

         // Computes the kind of the edges to 'h'.
         String t = exceptionHandler.desc == null ? "java/lang/Throwable" : exceptionHandler.desc;
         int kind = Frame.OBJECT | cw.addType(t);

         // h is an exception handler.
         h.setAsTarget();

         // Adds 'h' as a successor of labels between 'start' and 'end'.
         while (l != e) {
            // Creates an edge to 'h'.
            Edge b = new Edge(kind, h);

            // Adds it to the successors of 'l'.
            b.next = l.successors;
            l.successors = b;

            // Goes to the next label.
            l = l.successor;
         }

         exceptionHandler = exceptionHandler.next;
      }
   }

   // Creates and visits the first (implicit) frame.
   private void createAndVisitFirstFrame() {
      Type[] args = Type.getArgumentTypes(descriptor);
      Frame f = labels.frame;
      f.initInputFrame(cw, access, args, frameAndStack.maxLocals);
      frameAndStack.visitFrame(f);
   }

   /**
    * Fix point algorithm: mark the first basic block as 'changed' (i.e. put it in the 'changed' list) and, while there
    * are changed basic blocks, choose one, mark it as unchanged, and update its successors (which can be changed in the
    * process).
    */
   private int computeMaxStackSizeFromComputedFrames() {
      int max = 0;
      Label changed = labels;
      Frame f;

      while (changed != null) {
         // Removes a basic block from the list of changed basic blocks.
         Label l = changed;
         changed = changed.next;
         l.next = null;
         f = l.frame;

         // A reachable jump target must be stored in the stack map.
         if (l.isTarget()) {
            l.status |= Label.STORE;
         }

         // All visited labels are reachable, by definition.
         l.status |= Label.REACHABLE;

         // Updates the (absolute) maximum stack size.
         int blockMax = f.inputStack.length + l.outputStackMax;

         if (blockMax > max) {
            max = blockMax;
         }

         // Updates the successors of the current basic block.
         Edge e = l.successors;

         while (e != null) {
            Label n = e.successor.getFirst();
            boolean change = f.merge(cw, n.frame, e.info);

            if (change && n.next == null) {
               // If n has changed and is not already in the 'changed' list, adds it to this list.
               n.next = changed;
               changed = n;
            }

            e = e.next;
         }
      }

      return max;
   }

   // Visits all the frames that must be stored in the stack map.
   private int visitAllFramesToBeStoredInStackMap(int max) {
      Label label = labels;
      Frame frame;

      while (label != null) {
         frame = label.frame;

         if ((label.status & Label.STORE) != 0) {
            frameAndStack.visitFrame(frame);
         }

         if ((label.status & Label.REACHABLE) == 0) {
            // Finds start and end of dead basic block.
            Label k = label.successor;
            int start = label.position;
            int end = (k == null ? code.length : k.position) - 1;

            // If non empty basic block.
            if (end >= start) {
               max = Math.max(max, 1);

               // Replaces instructions with NOP ... NOP ATHROW.
               for (int i = start; i < end; ++i) {
                  code.data[i] = NOP;
               }

               code.data[end] = (byte) ATHROW;

               frameAndStack.emitFrameForUnreachableBlock(start);

               // Removes the start-end range from the exception handlers.
               firstExceptionHandler = ExceptionHandler.remove(firstExceptionHandler, label, k);
            }
         }

         label = label.successor;
      }

      return max;
   }

   private void countNumberOfHandlers() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;
      handlerCount = 0;

      while (exceptionHandler != null) {
         handlerCount++;
         exceptionHandler = exceptionHandler.next;
      }
   }

   private void completeControlFlowGraphWithExceptionHandlerBlocks() {
      ExceptionHandler exceptionHandler = firstExceptionHandler;

      while (exceptionHandler != null) {
         Label l = exceptionHandler.start;
         Label h = exceptionHandler.handler;
         Label e = exceptionHandler.end;

         // Adds 'h' as a successor of labels between 'start' and 'end'.
         while (l != e) {
            // Creates an edge to 'h'.
            Edge b = new Edge(Edge.EXCEPTION, h);

            // Adds it to the successors of 'l'.
            if (!l.isJSR()) {
               b.next = l.successors;
               l.successors = b;
            }
            else {
               // If l is a JSR block, adds b after the first two edges to preserve the hypothesis about JSR block
               // successors order (see {@link #visitJumpInsn}).
               b.next = l.successors.next.next;
               l.successors.next.next = b;
            }

            // Goes to the next label.
            l = l.successor;
         }

         exceptionHandler = exceptionHandler.next;
      }
   }

   /**
    * First step: finds the subroutines. This step determines, for each basic block, to which subroutine(s) it
    * belongs. Second step: finds the successors of RET blocks.
    */
   private void completeControlFlowGraphWithRETSuccessors() {
      // Finds the basic blocks that belong to the "main" subroutine.
      int id = 0;
      labels.visitSubroutine(null, 1, subroutines);

      // Finds the basic blocks that belong to the real subroutines.
      Label l = labels;

      while (l != null) {
         if (l.isJSR()) {
            // The subroutine is defined by l's TARGET, not by l.
            Label subroutine = l.successors.next.successor;

            // If this subroutine has not been visited yet...
            if (!subroutine.isVisited()) {
               // ...assigns it a new id and finds its basic blocks.
               id += 1;
               subroutine.visitSubroutine(null, (id / 32L) << 32 | (1L << (id % 32)), subroutines);
            }
         }

         l = l.successor;
      }

      // Second step: finds the successors of RET blocks.
      l = labels;

      while (l != null) {
         if (l.isJSR()) {
            Label L = labels;

            while (L != null) {
               L.status &= ~Label.VISITED2;
               L = L.successor;
            }

            // The subroutine is defined by l's TARGET, not by l.
            Label subroutine = l.successors.next.successor;
            subroutine.visitSubroutine(l, 0, subroutines);
         }

         l = l.successor;
      }
   }

   /**
    * Control flow analysis algorithm: while the block stack is not empty, pop a block from this stack, update the max
    * stack size, compute the true (non relative) begin stack size of the successors of this block, and push these
    * successors onto the stack (unless they have already been pushed onto the stack).
    * Note: by hypothesis, the {@link Label#inputStackTop} of the blocks in the block stack are the true (non relative)
    * beginning stack sizes of these blocks.
    */
   private int computeMaxStackSize() {
      int max = 0;
      Label stack = labels;

      while (stack != null) {
         // Pops a block from the stack.
         Label l = stack;
         stack = stack.next;

         // Computes the true (non relative) max stack size of this block.
         int start = l.inputStackTop;
         int blockMax = start + l.outputStackMax;

         // Updates the global max stack size.
         if (blockMax > max) {
            max = blockMax;
         }

         // Analyzes the successors of the block.
         Edge b = l.successors;

         if (l.isJSR()) {
            // Ignores the first edge of JSR blocks (virtual successor).
            b = b.next;
         }

         while (b != null) {
            l = b.successor;

            // If this successor has not already been pushed...
            if ((l.status & Label.PUSHED) == 0) {
               // computes its true beginning stack size...
               l.inputStackTop = b.info == Edge.EXCEPTION ? 1 : start + b.info;

               // ...and pushes it onto the stack.
               l.status |= Label.PUSHED;
               l.next = stack;
               stack = l;
            }

            b = b.next;
         }
      }

      return max;
   }

   // ------------------------------------------------------------------------
   // Utility methods: control flow analysis algorithm
   // ------------------------------------------------------------------------

   /**
    * Adds a successor to the {@link #currentBlock currentBlock} block.
    *
    * @param info      information about the control flow edge to be added.
    * @param successor the successor block to be added to the current block.
    */
   private void addSuccessor(int info, Label successor) {
      // Creates and initializes an Edge object...
      Edge b = new Edge(info, successor);

      // ...and adds it to the successor list of the currentBlock block.
      b.next = currentBlock.successors;
      currentBlock.successors = b;
   }

   /**
    * Ends the current basic block. This method must be used in the case where the current basic block does not have any
    * successor.
    */
   private void noSuccessor() {
      if (computeFrames) {
         Label l = new Label();
         l.frame = new Frame(l);
         l.resolve(code);
         previousBlock.successor = l;
         previousBlock = l;
      }
      else {
         currentBlock.outputStackMax = maxStackSize;
      }

      currentBlock = null;
   }

   // ------------------------------------------------------------------------
   // Utility methods: dump bytecode array
   // ------------------------------------------------------------------------

   /**
    * Returns the size of the bytecode of this method.
    */
   int getSize() {
      if (classReaderOffset != 0) {
         return 6 + classReaderLength;
      }

      int size = 8;

      if (code.length > 0) {
         if (code.length > 65536) {
            throw new RuntimeException("Method code too large!");
         }

         cw.newUTF8("Code");
         size += 18 + code.length + 8 * handlerCount;

         if (localVar != null) {
            cw.newUTF8("LocalVariableTable");
            size += 8 + localVar.length;
         }

         if (localVarType != null) {
            cw.newUTF8("LocalVariableTypeTable");
            size += 8 + localVarType.length;
         }

         if (lineNumber != null) {
            cw.newUTF8("LineNumberTable");
            size += 8 + lineNumber.length;
         }

         size += frameAndStack.getSizeWhileAddingConstantPoolItem();
      }

      if (exceptionCount > 0) {
         cw.newUTF8("Exceptions");
         size += 8 + 2 * exceptionCount;
      }

      if (cw.isSynthetic(access)) {
         cw.newUTF8("Synthetic");
         size += 6;
      }

      if ((access & ACC_DEPRECATED) != 0) {
         cw.newUTF8("Deprecated");
         size += 6;
      }

      if (signature != null) {
         cw.newUTF8("Signature");
         cw.newUTF8(signature);
         size += 8;
      }

      if (annotationDefault != null) {
         cw.newUTF8("AnnotationDefault");
         size += 6 + annotationDefault.length;
      }

      size += getAnnotationsSize(cw);

      if (parameterAnnotations != null) {
         cw.newUTF8("RuntimeVisibleParameterAnnotations");
         size += 7 + 2 * parameterAnnotations.length;

         for (int i = parameterAnnotations.length - 1; i >= 0; --i) {
            size += parameterAnnotations[i] == null ? 0 : parameterAnnotations[i].getSize();
         }
      }

      return size;
   }

   /**
    * Puts the bytecode of this method in the given byte vector.
    *
    * @param out the byte vector into which the bytecode of this method must be copied.
    */
   void put(ByteVector out) {
      int mask =
         ACC_CONSTRUCTOR | ACC_DEPRECATED | ACC_SYNTHETIC_ATTRIBUTE |
         ((access & ACC_SYNTHETIC_ATTRIBUTE) / TO_ACC_SYNTHETIC);
      out.putShort(access & ~mask).putShort(name).putShort(desc);

      if (classReaderOffset != 0) {
         out.putByteArray(cw.cr.b, classReaderOffset, classReaderLength);
         return;
      }

      int attributeCount = 0;

      if (code.length > 0) {
         ++attributeCount;
      }

      if (exceptionCount > 0) {
         ++attributeCount;
      }


      boolean synthetic = cw.isSynthetic(access);

      if (synthetic) {
         ++attributeCount;
      }

      if ((access & ACC_DEPRECATED) != 0) {
         ++attributeCount;
      }

      if (signature != null) {
         ++attributeCount;
      }

      if (annotationDefault != null) {
         ++attributeCount;
      }

      if (annotations != null) {
         ++attributeCount;
      }

      if (parameterAnnotations != null) {
         ++attributeCount;
      }

      out.putShort(attributeCount);

      if (code.length > 0) {
         int size = 12 + code.length + 8 * handlerCount;

         if (localVar != null) {
            size += 8 + localVar.length;
         }

         if (localVarType != null) {
            size += 8 + localVarType.length;
         }

         if (lineNumber != null) {
            size += 8 + lineNumber.length;
         }

         size += frameAndStack.getSize();

         out.putShort(cw.newUTF8("Code")).putInt(size);
         frameAndStack.putMaxStackAndLocals(out);
         out.putInt(code.length).putByteVector(code);
         out.putShort(handlerCount);

         if (handlerCount > 0) {
            ExceptionHandler h = firstExceptionHandler;

            while (h != null) {
               out.putShort(h.start.position).putShort(h.end.position);
               out.putShort(h.handler.position).putShort(h.type);
               h = h.next;
            }
         }

         attributeCount = 0;

         if (localVar != null) {
            ++attributeCount;
         }

         if (localVarType != null) {
            ++attributeCount;
         }

         if (lineNumber != null) {
            ++attributeCount;
         }

         if (frameAndStack.hasStackMap()) {
            ++attributeCount;
         }

         out.putShort(attributeCount);

         if (localVar != null) {
            out.putShort(cw.newUTF8("LocalVariableTable"));
            out.putInt(localVar.length + 2).putShort(localVarCount);
            out.putByteVector(localVar);
         }

         if (localVarType != null) {
            out.putShort(cw.newUTF8("LocalVariableTypeTable"));
            out.putInt(localVarType.length + 2).putShort(localVarTypeCount);
            out.putByteVector(localVarType);
         }

         if (lineNumber != null) {
            out.putShort(cw.newUTF8("LineNumberTable"));
            out.putInt(lineNumber.length + 2).putShort(lineNumberCount);
            out.putByteVector(lineNumber);
         }

         frameAndStack.put(out);
      }

      if (exceptionCount > 0) {
         out.putShort(cw.newUTF8("Exceptions")).putInt(2 * exceptionCount + 2);
         out.putShort(exceptionCount);

         for (int i = 0; i < exceptionCount; ++i) {
            out.putShort(exceptions[i]);
         }
      }

      if (synthetic) {
         out.putShort(cw.newUTF8("Synthetic")).putInt(0);
      }

      if ((access & ACC_DEPRECATED) != 0) {
         out.putShort(cw.newUTF8("Deprecated")).putInt(0);
      }

      if (signature != null) {
         out.putShort(cw.newUTF8("Signature")).putInt(2).putShort(cw.newUTF8(signature));
      }

      if (annotationDefault != null) {
         out.putShort(cw.newUTF8("AnnotationDefault"));
         out.putInt(annotationDefault.length);
         out.putByteVector(annotationDefault);
      }

      putAnnotations(out, cw);

      if (parameterAnnotations != null) {
         out.putShort(cw.newUTF8("RuntimeVisibleParameterAnnotations"));
         AnnotationWriter.put(parameterAnnotations, out);
      }
   }
}
