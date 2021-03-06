
package org.jruby.ext.ffi.jna;

import java.io.File;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyConstant;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Pointer;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "FFI::DynamicLibrary", parent = "Object")
public class DynamicLibrary extends RubyObject {
    
    @JRubyConstant public static final int RTLD_LAZY   = 0x00001;
    @JRubyConstant public static final int RTLD_NOW    = 0x00002;
    @JRubyConstant public static final int RTLD_LOCAL  = 0x00004;
    @JRubyConstant public static final int RTLD_GLOBAL = 0x00008;
    
    private final com.sun.jna.NativeLibrary library;
    private final String name;
    public static RubyClass createDynamicLibraryClass(Ruby runtime, RubyModule module) {
        RubyClass result = module.defineClassUnder("DynamicLibrary",
                runtime.getObject(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        RubyClass symClass = result.defineClassUnder("Symbol",
                module.fastGetClass("Pointer"), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        symClass.defineAnnotatedMethods(Symbol.class);
        result.defineAnnotatedMethods(DynamicLibrary.class);
        result.defineAnnotatedConstants(DynamicLibrary.class);

        return result;
    }
    public DynamicLibrary(Ruby runtime, RubyClass klass, String name, com.sun.jna.NativeLibrary library) {
        super(runtime, klass);
        this.name = name;
        this.library = library;
    }

    com.sun.jna.NativeLibrary getNativeLibrary() {
        return library;
    }
    @JRubyMethod(name = {  "open" }, meta = true)
    public static final  IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject libraryName, IRubyObject libraryFlags) {
        String libName = !libraryName.isNil() ? libraryName.toString() : Platform.LIBC;
        if (libName.indexOf(File.separatorChar) != -1) {
            libName = new File(libName).getAbsolutePath();
        }
        try {
            return new DynamicLibrary(context.getRuntime(), (RubyClass) recv, 
                    libName, com.sun.jna.NativeLibrary.getInstance(libName));
        } catch (UnsatisfiedLinkError ex) {
            throw context.getRuntime().newLoadError(ex.getMessage());
        }
    }
    
    @JRubyMethod(name = {  "find_function" })
    public IRubyObject findFunction(ThreadContext context, IRubyObject symbolName) {
        final String sym = symbolName.toString();
        try {
            return new Symbol(context.getRuntime(), this, sym, library.getFunction(sym));
        } catch (UnsatisfiedLinkError ex) {
            return context.getRuntime().getNil();
        }
    }

    @JRubyMethod(name = {  "find_symbol", "find_variable" })
    public IRubyObject findVariable(ThreadContext context, IRubyObject symbolName) {
        final String sym = symbolName.toString();
        try {
            return new Symbol(context.getRuntime(), this, sym, library.getGlobalVariableAddress(sym));
        } catch (UnsatisfiedLinkError ex) {
            return context.getRuntime().getNil();
        }
    }
    @JRubyMethod(name = "name")
    public IRubyObject name(ThreadContext context) {
        return RubyString.newString(context.getRuntime(), name);
    }
    public static final class Symbol extends Pointer {
        private final DynamicLibrary library;
        private final com.sun.jna.Pointer address;
        private final String name;

        public Symbol(Ruby runtime, DynamicLibrary library, String name, com.sun.jna.Pointer address) {
            super(runtime, runtime.fastGetModule("FFI").fastGetClass("DynamicLibrary").fastGetClass("Symbol"),
                    new NativeMemoryIO(runtime, address), Long.MAX_VALUE);
            this.library = library;
            this.address = address;
            this.name = name;
        }

        com.sun.jna.NativeLibrary getNativeLibrary() {
            return library.getNativeLibrary();
        }

        com.sun.jna.Pointer getNativeAddress() {
            return address;
        }

        @JRubyMethod(name = "library")
        public IRubyObject library(ThreadContext context) {
            return library;
        }

        @Override
        @JRubyMethod(name = "inspect")
        public IRubyObject inspect(ThreadContext context) {
            return RubyString.newString(context.getRuntime(),
                    String.format("#<Library Symbol library=%s symbol=%s address=%#x>", 
                    library.name, name, getAddress()));
        }

        @Override
        public final String toString() {
            return name;
        }

        final String getName() {
            return name;
        }
    }
}
