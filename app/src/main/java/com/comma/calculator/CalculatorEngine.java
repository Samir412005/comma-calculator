package com.comma.calculator;

import java.math.BigDecimal;
import java.math.MathContext;

/** Decimal calculator; no floating point in basic arithmetic. */
public final class CalculatorEngine {
    private static final MathContext MC=MathContext.DECIMAL128;
    public String expression="",history="";
    public boolean evaluated=false,error=false;
    public void clear(){expression="";history="";evaluated=false;error=false;}
    public void digit(char c){
        if(evaluated||error)clear();
        if(expression.length()>=180||expression.endsWith("%"))return;
        int start=expression.length();
        while(start>0&&(Character.isDigit(expression.charAt(start-1))||expression.charAt(start-1)=='.'))start--;
        String number=expression.substring(start);
        if(number.length()>=24)return;
        if(c=='.'){if(number.contains("."))return;if(number.isEmpty())expression+="0";}
        else if(number.equals("0"))expression=expression.substring(0,start);
        expression+=c;
    }
    public void operator(char c){
        if(error||expression.isEmpty())return;
        evaluated=false;
        while(!expression.isEmpty()&&isOperator(expression.charAt(expression.length()-1)))expression=expression.substring(0,expression.length()-1);
        if(!expression.isEmpty()&&expression.length()<180)expression+=c;
    }
    public void percent(){
        if(error||expression.isEmpty())return;
        char end=expression.charAt(expression.length()-1);
        if((Character.isDigit(end)||end=='.')&&expression.length()<180){expression+="%";evaluated=false;}
    }
    public void negate(){
        if(error)clear();evaluated=false;int end=expression.length();
        if(end==0||isOperator(expression.charAt(end-1))){
            if(end>0&&expression.charAt(end-1)=='-'&&(end==1||isOperator(expression.charAt(end-2))))expression=expression.substring(0,end-1);
            else if(end<180)expression+="-";return;
        }
        int start=end;
        while(start>0&&(Character.isDigit(expression.charAt(start-1))||expression.charAt(start-1)=='.'||expression.charAt(start-1)=='%'))start--;
        if(start>0&&expression.charAt(start-1)=='-'&&(start==1||isOperator(expression.charAt(start-2))))expression=expression.substring(0,start-1)+expression.substring(start);
        else if(expression.length()<180)expression=expression.substring(0,start)+"-"+expression.substring(start);
    }
    public void backspace(){if(error){clear();return;}if(!expression.isEmpty())expression=expression.substring(0,expression.length()-1);evaluated=false;}
    public String equalsResult(){if(expression.isEmpty())return "0";if(evaluated&&!error)return expression;return compute("");}
    /** Functions apply to the value of the complete visible expression. */
    public String function(String name){if(error)return "امسح الخطأ أو أدخل رقماً";return compute(name);}
    private String compute(String function){
        try{
            String old=expression.isEmpty()?"0":expression;
            BigDecimal result=evaluate(old);String caption=old;
            if(function.equals("sqrt")){
                if(result.signum()<0)throw new DomainException();
                caption="√("+old+")";result=sqrt(result);
            }else if(function.equals("square")){caption="("+old+")²";result=result.multiply(result,MC);}
            else if(function.equals("inverse")){caption="1 ÷ ("+old+")";result=BigDecimal.ONE.divide(result,MC);}
            String formatted=format(result);history=caption+" =";expression=formatted;evaluated=true;error=false;return formatted;
        }catch(DomainException ex){error=true;return "لا جذر حقيقي لعدد سالب";}
        catch(ResultRangeException ex){error=true;return "النتيجة خارج النطاق";}
        catch(ArithmeticException ex){error=true;return "لا يمكن القسمة على صفر";}
        catch(IllegalArgumentException ex){error=true;return "أكمل العملية أولاً";}
    }
    public static BigDecimal sqrt(BigDecimal n){
        if(n.signum()<0)throw new DomainException();if(n.signum()==0)return BigDecimal.ZERO;
        // Double is used only for a starting estimate; decimal Newton refinement computes the result.
        BigDecimal x=BigDecimal.valueOf(Math.sqrt(n.doubleValue()));
        if(x.signum()==0)x=BigDecimal.ONE.scaleByPowerOfTen((n.precision()-n.scale())/2);
        for(int i=0;i<24;i++){
            BigDecimal next=x.add(n.divide(x,MC),MC).divide(new BigDecimal("2"),MC);
            if(next.compareTo(x)==0)return next;x=next;
        }
        return x;
    }
    public static boolean isOperator(char c){return c=='+'||c=='-'||c=='*'||c=='/';}
    public static String format(BigDecimal n){if(n.signum()==0)return "0";String s=n.stripTrailingZeros().toPlainString();if(s.length()>160)throw new ResultRangeException();return s;}
    public static BigDecimal evaluate(String text){Parser p=new Parser(text);BigDecimal result=p.sum();if(p.position!=text.length())throw new IllegalArgumentException();return result;}
    private static final class DomainException extends IllegalArgumentException{}
    private static final class ResultRangeException extends IllegalArgumentException{}
    private static final class Parser{
        final String text;int position;
        Parser(String s){text=s;}
        boolean take(char c){if(position<text.length()&&text.charAt(position)==c){position++;return true;}return false;}
        BigDecimal sum(){BigDecimal v=product();while(position<text.length()){if(take('+'))v=v.add(product(),MC);else if(take('-'))v=v.subtract(product(),MC);else break;}return v;}
        BigDecimal product(){BigDecimal v=number();while(position<text.length()){if(take('*'))v=v.multiply(number(),MC);else if(take('/'))v=v.divide(number(),MC);else break;}return v;}
        BigDecimal number(){boolean neg=take('-');int start=position;while(position<text.length()&&(Character.isDigit(text.charAt(position))||text.charAt(position)=='.'))position++;if(start==position)throw new IllegalArgumentException();BigDecimal v;try{v=new BigDecimal(text.substring(start,position),MC);}catch(NumberFormatException ex){throw new IllegalArgumentException(ex);}if(neg)v=v.negate();if(take('%'))v=v.divide(new BigDecimal("100"),MC);return v;}
    }
}
