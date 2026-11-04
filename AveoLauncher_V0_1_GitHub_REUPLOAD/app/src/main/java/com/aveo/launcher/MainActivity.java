package com.aveo.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  final int bg=Color.rgb(8,15,27), panel=Color.rgb(20,32,50), accent=Color.rgb(48,164,245), muted=Color.rgb(169,190,211);
  int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));d.setStroke(dp(1),Color.rgb(43,66,91));return d;}
  TextView label(String s,int sp,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
  LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  void add(LinearLayout p,View v,int w,int h,float weight){LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h),weight);a.setMargins(dp(4),dp(4),dp(4),dp(4));p.addView(v,a);}
  LinearLayout card(String title,String value){LinearLayout l=col();l.setBackground(shape(panel));l.setPadding(dp(13),dp(10),dp(13),dp(10));add(l,label(title,12,muted),-1,-2,0);add(l,label(value,17,Color.WHITE),-1,-2,0);return l;}
  Button btn(String text,Runnable action){Button b=new Button(this);b.setText(text);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackground(shape(panel));b.setOnClickListener(v->action.run());return b;}
  List<ResolveInfo> apps(){Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);List<ResolveInfo> list=getPackageManager().queryIntentActivities(i,0);Collections.sort(list,(a,b)->a.loadLabel(getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(getPackageManager()).toString()));return list;}
  void start(ResolveInfo a){try{Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);i.setClassName(a.activityInfo.packageName,a.activityInfo.name);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);}catch(Exception e){Toast.makeText(this,"Impossible d'ouvrir l'application",Toast.LENGTH_SHORT).show();}}
  void launch(String... names){for(ResolveInfo a:apps()){String n=a.loadLabel(getPackageManager()).toString().toLowerCase(Locale.ROOT);for(String q:names)if(n.contains(q.toLowerCase(Locale.ROOT))){start(a);return;}}Toast.makeText(this,"Application non trouvée",Toast.LENGTH_SHORT).show();}
  void allApps(){List<ResolveInfo> list=apps();String[] names=new String[list.size()];for(int i=0;i<list.size();i++)names[i]=list.get(i).loadLabel(getPackageManager()).toString();new AlertDialog.Builder(this).setTitle("Applications").setItems(names,(d,index)->start(list.get(index))).setNegativeButton("Fermer",null).show();}
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
    LinearLayout root=new LinearLayout(this);root.setOrientation(0);root.setBackgroundColor(bg);root.setPadding(dp(5),dp(5),dp(5),dp(5));
    LinearLayout left=col();add(left,label("AVEO",22,accent),-1,48,0);add(left,btn("Accueil",()->{}),-1,52,0);add(left,btn("TLink5",()->launch("tlink")),-1,52,0);add(left,btn("Navigation",()->launch("maps","navigation")),-1,52,0);add(left,btn("Radio FM",()->launch("radio")),-1,52,0);add(left,btn("Musique",()->launch("musique","music")),-1,52,0);add(left,btn("Applications",this::allApps),-1,52,0);add(root,left,128,-1,0);
    LinearLayout middle=col();middle.setBackground(shape(panel));middle.setPadding(dp(15),dp(10),dp(15),dp(10));TextView head=label("CHEVROLET AVEO T250",20,Color.WHITE);head.setTypeface(null,Typeface.BOLD);add(middle,head,-1,42,0);add(middle,label("Hatchback 5 portes  •  Blanc",12,muted),-1,24,0);
    LinearLayout scene=col();scene.setBackground(shape(Color.rgb(12,24,40)));scene.setGravity(Gravity.CENTER);TextView placeholder=label("AVEO 3D\n\nModèle interactif à intégrer",20,accent);placeholder.setGravity(Gravity.CENTER);add(scene,placeholder,-1,-2,0);add(middle,scene,-1,0,1);add(middle,label("Zone 3D réservée — V0.2",12,muted),-1,26,0);add(root,middle,0,-1,1);
    LinearLayout right=col();LinearLayout clock=col();clock.setPadding(dp(12),dp(8),dp(12),dp(8));clock.setBackground(shape(panel));add(clock,label("TABLEAU DE BORD",12,muted),-1,28,0);TextClock time=new TextClock(this);time.setFormat24Hour("HH:mm");time.setTextSize(34);time.setTextColor(Color.WHITE);time.setGravity(Gravity.CENTER);add(clock,time,-1,58,0);add(right,clock,-1,-2,0);add(right,card("Tension véhicule","Source à identifier"),-1,-2,0);add(right,card("Vitesse GPS","Non connectée"),-1,-2,0);add(right,card("Informations moteur","OBD non connecté"),-1,-2,0);add(right,btn("Android Auto / TLink5",()->launch("tlink")),-1,48,0);add(right,btn("Toutes les applications",this::allApps),-1,48,0);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(right);add(root,scroll,238,-1,0);setContentView(root);
  }
}
