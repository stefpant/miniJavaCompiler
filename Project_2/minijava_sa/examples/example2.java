class Test1 {
    public static void main(String[] x) {
            boolean rv;
            A a;
            B b;
            b = new B();
        }
}

class A{
    int i;
    boolean flag;
    int j;

    public int foo() {int x;
    x = this.afoo(this.bfoo(1,this.afoo(this.cfoo(this.afoo(1), this.bfoo(2,2), this.afoo(3)))));
        return 0;}
    public int afoo(int af) {int x;

    return 0;}
    public int bfoo(int af, int bf) {int x; return 0;}
    public int cfoo(int af, int bf, int cf) {int x; return 0;}
    public A dfoo(int af, int bf, int cf, A df) {int x; return df;}
    public A ffoo(A df) {int x; return df;}
    public int efoo(int af, int bf, int cf, A df) {int x; x=21; return x;}

    public boolean fa() {int x; return true;}
}

class B extends A{
    A type ;
    int k;
    public int foo() {int x;
        A a; // call on call
        a = type.dfoo(
                type.foo(),
                1,
                type.foo(),
                ((type.ffoo(type)).ffoo(type)).ffoo( (type.dfoo(k, 1, 2, type.ffoo(type))).dfoo(type.bfoo(1,2), type.cfoo(type.afoo(1), 2, 3), 3, type.ffoo(type))) );
    return 0;}
    public boolean bla() {int x; return true;}
}
