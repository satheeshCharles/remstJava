public class Main {

    public static void main(String[] args) {
        int fNumber = 10;
        int sNumber = 20;
        int newobject;
        testingMethod();
        newobject = new testingMethod();
        newobject.fnumber=12;
        System.out.println(newobject.fnumber);
    }

    public static void testingMethod() {
        int fNumber = 10;
        int sNumber = 20;

        if (fNumber == 10) {
            System.out.println("Executing the if block");
            fNumber += 100;
            System.out.println(fNumber);
        } else if (fNumber < 20) {
            System.out.println("executing 1st else if");
        } else if (fNumber > 8) {
            System.out.println("executing second else if");
        } else {
            System.out.println("Printing Else");
        }
    }
}