# Generating Intermediate Code (MiniJava -> LLVM)

## Compile and Run (and install clang-4.0)
* sudo apt install clang-4.0
* make
* java Main file1.java ... fileN.java (generate ir code saved as fileX.ll)
* clang-4.0 -o outX fileX.ll
* ./outX

### In Project_2 can be found example files to use as inputs
