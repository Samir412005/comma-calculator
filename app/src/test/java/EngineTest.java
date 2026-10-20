import com.comma.calculator.CalculatorEngine;
import java.math.BigDecimal;
public class EngineTest {
 static int tests;
 static void eq(String e,String a){tests++;if(!e.equals(a))throw new AssertionError("expected "+e+", got "+a);}
 static void calc(String input,String expected){eq(expected,CalculatorEngine.format(CalculatorEngine.evaluate(input)));}
 static CalculatorEngine typed(String s){CalculatorEngine e=new CalculatorEngine();for(char c:s.toCharArray()){if(c=='%')e.percent();else if(CalculatorEngine.isOperator(c))e.operator(c);else e.digit(c);}return e;}
 public static void main(String[] args){
  calc("2+3*4","14");calc("20/5*2","8");calc("9-4-2","3");calc("0.1+0.2","0.3");calc("7/2","3.5");calc("-5*-2","10");calc("12+-3","9");calc("200*10%","20");calc("200+10%","200.1");calc("-50%","-0.5");calc("1/3","0.3333333333333333333333333333333333");calc("999999999999999999999999+1","1000000000000000000000000");
  CalculatorEngine e=typed("12.3.4");eq("12.34",e.expression);e.backspace();eq("12.3",e.expression);e.clear();eq("",e.expression);e=typed("0005");eq("5",e.expression);e=typed(".5");eq("0.5",e.expression);
  e=typed("2+3*4");eq("14",e.equalsResult());e.operator('+');e.digit('1');eq("15",e.equalsResult());e.digit('7');eq("7",e.expression);
  e=typed("5");e.negate();eq("-5",e.expression);e.negate();eq("5",e.expression);e=typed("2*3");e.negate();eq("2*-3",e.expression);eq("-6",e.equalsResult());e=typed("2+");e.negate();e.digit('3');eq("-1",e.equalsResult());e=typed("2+");e.operator('*');e.digit('3');eq("6",e.equalsResult());
  e=typed("50%");e.percent();eq("50%",e.expression);e.negate();eq("-0.5",e.equalsResult());e=typed("5/0");eq("لا يمكن القسمة على صفر",e.equalsResult());eq("true",String.valueOf(e.error));e.digit('2');eq("2",e.expression);eq("false",String.valueOf(e.error));e=typed("5+");eq("أكمل العملية أولاً",e.equalsResult());e.backspace();eq("",e.expression);e=new CalculatorEngine();e.negate();eq("-",e.expression);e.negate();eq("",e.expression);e=typed("5-3");e.negate();eq("5--3",e.expression);eq("8",e.equalsResult());e=typed("0.1+0.2");eq("0.3",e.equalsResult());eq("0.3",e.equalsResult());
  e=typed("7*8");eq("56",e.equalsResult());e=typed("81");eq("9",e.function("sqrt"));e=typed("0");eq("0",e.function("sqrt"));e=typed("2");eq("1.414213562373095048801688724209698",e.function("sqrt"));e=typed("0.0004");eq("0.02",e.function("sqrt"));e=typed("9");e.negate();eq("لا جذر حقيقي لعدد سالب",e.function("sqrt"));eq("true",String.valueOf(e.error));e=typed("7");eq("49",e.function("square"));e=typed("4");eq("0.25",e.function("inverse"));e=typed("0");eq("لا يمكن القسمة على صفر",e.function("inverse"));e=typed("2+7");eq("3",e.function("sqrt"));eq("√(2+7) =",e.history);e=typed("9");e.negate();eq("81",e.function("square"));e=typed("8");eq("0.125",e.function("inverse"));e.operator('*');e.digit('8');eq("1",e.equalsResult());
  System.out.println("PASS: "+tests+" assertions");
 }
}
