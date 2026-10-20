package com.comma.calculator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.text.DateFormat;
import java.util.Date;

public final class MainActivity extends Activity {
    private final CalculatorEngine engine=new CalculatorEngine();
    private SharedPreferences prefs;
    private JSONArray records=new JSONArray();
    private TextView display,previous;
    private HorizontalScrollView displayScroll;
    private String errorText="";
    private boolean dark;
    private int bg,surface,key,ink,muted,accent,accentInk,operatorBg;
    private int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+0.5f);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private LinearLayout vertical(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("calculator",MODE_PRIVATE);
        try{records=new JSONArray(prefs.getString("records","[]"));}catch(JSONException ex){records=new JSONArray();}
        int mode=prefs.getInt("theme",0);
        dark=mode==2||(mode==0&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        bg=Color.parseColor(dark?"#0C141E":"#F3F6FA");surface=Color.parseColor(dark?"#142130":"#FFFFFF");key=Color.parseColor(dark?"#1E2D40":"#FFFFFF");ink=Color.parseColor(dark?"#EEF4FB":"#152337");muted=Color.parseColor(dark?"#9EADC0":"#64748B");accent=Color.parseColor(dark?"#65E6CD":"#007C72");accentInk=Color.parseColor(dark?"#07382F":"#FFFFFF");operatorBg=Color.parseColor(dark?"#1B3C43":"#D9EEEB");
        if(state!=null){engine.expression=state.getString("expression","");engine.history=state.getString("history","");engine.evaluated=state.getBoolean("evaluated");engine.error=state.getBoolean("error");errorText=state.getString("errorText","");}
        else{engine.expression=prefs.getString("draft","");engine.history=prefs.getString("last","");engine.evaluated=prefs.getBoolean("evaluated",false);}
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        int flags=0;if(!dark&&android.os.Build.VERSION.SDK_INT>=23)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;if(!dark&&android.os.Build.VERSION.SDK_INT>=26)flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;getWindow().getDecorView().setSystemUiVisibility(flags);
        boolean landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root=new LinearLayout(this);root.setOrientation(landscape?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);root.setBackgroundColor(bg);
        root.setOnApplyWindowInsetsListener((v,insets)->{int top,bottom,left,right;if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());top=i.top;bottom=i.bottom;left=i.left;right=i.right;}else{top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();left=insets.getSystemWindowInsetLeft();right=insets.getSystemWindowInsetRight();}v.setPadding(dp(10)+left,dp(4)+top,dp(10)+right,dp(6)+bottom);return insets;});
        LinearLayout panel=vertical();root.addView(panel,landscape?new LinearLayout.LayoutParams(0,-1,1.2f):new LinearLayout.LayoutParams(-1,0,1.2f));
        LinearLayout toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);panel.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView title=text("آلة حاسبة",20,ink);title.setTypeface(null,Typeface.BOLD);title.setPadding(dp(8),0,dp(8),0);toolbar.addView(title,new LinearLayout.LayoutParams(0,-1,1));
        Button history=button("السجل",13,"سجل العمليات",surface,ink);toolbar.addView(history,new LinearLayout.LayoutParams(dp(66),dp(42)));history.setOnClickListener(v->{feedback(v);showHistory();});
        Button theme=button("المظهر",13,"تغيير المظهر",surface,ink);LinearLayout.LayoutParams themeParams=new LinearLayout.LayoutParams(dp(68),dp(42));themeParams.setMargins(dp(5),0,dp(5),0);toolbar.addView(theme,themeParams);theme.setOnClickListener(v->{feedback(v);showTheme();});
        LinearLayout screen=vertical();screen.setGravity(Gravity.BOTTOM);screen.setPadding(dp(14),dp(4),dp(14),dp(6));screen.setBackground(shape(surface,20));LinearLayout.LayoutParams screenParams=new LinearLayout.LayoutParams(-1,0,1);screenParams.setMargins(dp(4),dp(6),dp(4),dp(4));panel.addView(screen,screenParams);
        previous=text("",14,muted);previous.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);previous.setSingleLine(true);previous.setTextDirection(View.TEXT_DIRECTION_LTR);screen.addView(previous,new LinearLayout.LayoutParams(-1,dp(23)));
        displayScroll=new HorizontalScrollView(this);displayScroll.setFillViewport(true);displayScroll.setHorizontalScrollBarEnabled(false);displayScroll.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        display=text("0",38,ink);display.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);display.setSingleLine(true);display.setTextDirection(View.TEXT_DIRECTION_LTR);display.setTextIsSelectable(true);display.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);displayScroll.addView(display,new HorizontalScrollView.LayoutParams(-1,-1));screen.addView(displayScroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView note=text("% = ÷100  ·  الدوال تُطبّق على العملية كاملة",10,muted);note.setGravity(Gravity.CENTER);panel.addView(note,new LinearLayout.LayoutParams(-1,dp(20)));
        LinearLayout keypad=vertical();keypad.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);root.addView(keypad,landscape?new LinearLayout.LayoutParams(0,-1,2):new LinearLayout.LayoutParams(-1,0,3.2f));
        String[][] rows={{"√","x²","1/x","⌫"},{"C","±","%","÷"},{"7","8","9","×"},{"4","5","6","−"},{"1","2","3","+"},{"00","0",".","="}};
        for(String[] labels:rows){LinearLayout row=new LinearLayout(this);keypad.addView(row,new LinearLayout.LayoutParams(-1,0,1));for(String label:labels){boolean op=label.equals("+")||label.equals("−")||label.equals("×")||label.equals("÷");boolean special=label.equals("√")||label.equals("x²")||label.equals("1/x")||label.equals("⌫")||label.equals("C")||label.equals("±")||label.equals("%");int color=label.equals("=")?accent:op?operatorBg:special?surface:key;int foreground=label.equals("=")?accentInk:op?accent:label.equals("C")?Color.parseColor(dark?"#FDA4AF":"#B42338"):ink;Button b=button(label,special?21:26,description(label),color,foreground);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(dp(4),dp(4),dp(4),dp(4));row.addView(b,p);b.setOnClickListener(v->{feedback(v);press(label);});}}
        setContentView(root);render();root.requestApplyInsets();
    }
    private Button button(String label,int size,String description,int color,int foreground){Button b=new Button(this);b.setText(label);b.setTextSize(size);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(foreground);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(0,0,0,0);b.setBackground(new RippleDrawable(ColorStateList.valueOf(dark?0x4465E6CD:0x33007C72),shape(color,16),null));b.setContentDescription(description);return b;}
    private void feedback(View v){v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);}
    private String description(String k){switch(k){case "√":return "جذر تربيعي";case "x²":return "تربيع";case "1/x":return "مقلوب العدد";case "⌫":return "حذف آخر محرف";case "C":return "مسح الكل";case "±":return "تغيير الإشارة";case "%":return "نسبة مئوية";case "÷":return "قسمة";case "×":return "ضرب";case "−":return "طرح";case "+":return "جمع";case "=":return "يساوي";case ".":return "فاصلة عشرية";default:return k;}}
    private void press(String k){boolean record=false;
        switch(k){case "C":engine.clear();break;case "⌫":engine.backspace();break;case "±":engine.negate();break;case "%":engine.percent();break;case "=":record=!engine.expression.isEmpty()&&(!engine.evaluated||engine.error);errorText=engine.equalsResult();break;case "√":errorText=engine.function("sqrt");record=true;break;case "x²":errorText=engine.function("square");record=true;break;case "1/x":errorText=engine.function("inverse");record=true;break;case "+":engine.operator('+');break;case "−":engine.operator('-');break;case "×":engine.operator('*');break;case "÷":engine.operator('/');break;case "00":engine.digit('0');engine.digit('0');break;default:engine.digit(k.charAt(0));}
        if(record&&!engine.error)addRecord();render();displayScroll.post(()->displayScroll.fullScroll(View.FOCUS_RIGHT));
    }
    private String pretty(String s){return s.replace('*','×').replace('/','÷').replace('-','−');}
    private void render(){previous.setText(pretty(engine.history));display.setText(engine.error?errorText:engine.expression.isEmpty()?"0":pretty(engine.expression));display.setTextSize(engine.error?20:engine.expression.length()>18?27:38);display.setTextColor(engine.error?Color.parseColor(dark?"#FDA4AF":"#B42338"):ink);}
    private void addRecord(){try{JSONObject item=new JSONObject();item.put("e",engine.history);item.put("r",engine.expression);item.put("t",System.currentTimeMillis());JSONArray next=new JSONArray();next.put(item);for(int i=0;i<records.length()&&i<59;i++)next.put(records.get(i));records=next;prefs.edit().putString("records",records.toString()).apply();}catch(JSONException ignored){}}
    private AlertDialog.Builder dialog(){return new AlertDialog.Builder(new ContextThemeWrapper(this,dark?android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert));}
    private void showTheme(){String[] names={"إعداد النظام","فاتح","داكن"};dialog().setTitle("المظهر").setSingleChoiceItems(names,prefs.getInt("theme",0),(d,which)->{prefs.edit().putInt("theme",which).apply();d.dismiss();recreate();}).setNegativeButton("إغلاق",null).show();}
    private void showHistory(){
        LinearLayout list=vertical();list.setPadding(dp(16),dp(8),dp(16),dp(8));list.setBackgroundColor(surface);list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if(records.length()==0){TextView empty=text("لا توجد عمليات محفوظة بعد",17,muted);empty.setPadding(0,dp(24),0,dp(24));list.addView(empty);}
        final AlertDialog[] owner=new AlertDialog[1];
        for(int i=0;i<records.length();i++){JSONObject item=records.optJSONObject(i);if(item==null)continue;String result=item.optString("r","0");LinearLayout entry=vertical();entry.setPadding(dp(12),dp(10),dp(12),dp(10));entry.setBackground(new RippleDrawable(ColorStateList.valueOf(0x3365E6CD),shape(key,12),null));TextView expr=text(pretty(item.optString("e","")),14,muted);expr.setTextDirection(View.TEXT_DIRECTION_LTR);TextView value=text(result,24,accent);value.setTextDirection(View.TEXT_DIRECTION_LTR);value.setTypeface(null,Typeface.BOLD);TextView time=text(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(item.optLong("t"))),11,muted);entry.addView(expr);entry.addView(value);entry.addView(time);entry.setContentDescription("استعادة النتيجة "+result);entry.setOnClickListener(v->{feedback(v);engine.clear();engine.expression=result;engine.evaluated=true;render();owner[0].dismiss();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(8);list.addView(entry,p);}
        ScrollView scroll=new ScrollView(this);scroll.addView(list);owner[0]=dialog().setTitle("سجل العمليات · "+records.length()).setView(scroll).setNegativeButton("إغلاق",null).setNeutralButton("مسح السجل",(d,w)->{dialog().setTitle("مسح السجل؟").setMessage("ستُحذف العمليات المحفوظة على هذا الجهاز.").setPositiveButton("مسح",(a,b)->{records=new JSONArray();prefs.edit().putString("records","[]").apply();}).setNegativeButton("إلغاء",null).show();}).create();owner[0].show();
    }
    @Override protected void onSaveInstanceState(Bundle s){super.onSaveInstanceState(s);s.putString("expression",engine.expression);s.putString("history",engine.history);s.putBoolean("evaluated",engine.evaluated);s.putBoolean("error",engine.error);s.putString("errorText",errorText);}
    @Override protected void onStop(){super.onStop();if(prefs!=null)prefs.edit().putString("draft",engine.error?"":engine.expression).putString("last",engine.history).putBoolean("evaluated",engine.evaluated&&!engine.error).apply();}
}
