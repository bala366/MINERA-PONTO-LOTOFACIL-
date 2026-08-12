package br.com.mineradorlotofacil.oito;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final int PURPLE=Color.rgb(164,90,194), DARK=Color.rgb(106,27,154),
        BG=Color.rgb(250,247,252), TEXT=Color.rgb(35,35,35), WHITE=Color.WHITE;

    final ExecutorService executor=Executors.newSingleThreadExecutor();
    ArrayList<int[]> history=new ArrayList<>();
    ArrayList<CoreEngine.AldeotaEntry> aldeota=new ArrayList<>();
    CoreEngine.Model model;

    LinearLayout root,content;
    TextView status;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(DARK);
        dashboard();
    }
    @Override protected void onDestroy(){super.onDestroy();executor.shutdownNow();}

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    LinearLayout vbox(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    TextView tv(String s,int z,boolean bold,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(color);
        t.setPadding(dp(10),dp(8),dp(10),dp(8));if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);return t;
    }
    Button button(String s){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(WHITE);b.setBackgroundColor(PURPLE);return b;
    }
    LinearLayout.LayoutParams match(int h){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,h);p.setMargins(0,dp(5),0,dp(5));return p;
    }
    void dashboard(){
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setBackgroundColor(BG);
        root=vbox();root.setPadding(dp(14),dp(10),dp(14),dp(40));sv.addView(root);

        LinearLayout head=vbox();head.setPadding(dp(16),dp(14),dp(16),dp(14));head.setBackgroundColor(PURPLE);
        head.addView(tv("☘  MINERADOR LOTOFÁCIL 8M",26,true,WHITE));
        head.addView(tv("8 módulos • FILTRO FIXO: somente 8, 9 ou 10 repetidas • PDF + justificativa",14,false,WHITE));
        root.addView(head,new LinearLayout.LayoutParams(-1,-2));

        TextView baseTitle=tv("FUNCIONAMENTO INICIAL — CARREGAR TXT",18,true,DARK);root.addView(baseTitle);
        Button load=button("CARREGAR TXT DE RESULTADOS");root.addView(load,match(dp(60)));
        Button loadA=button("CARREGAR BASE DOS BOLÕES DA ALDEOTA");root.addView(loadA,match(dp(60)));
        status=tv("TXT ainda não carregado. Isso é funcionamento inicial, não é módulo.",14,false,TEXT);root.addView(status);
        load.setOnClickListener(v->pick(10));loadA.setOnClickListener(v->pick(11));

        root.addView(tv("MÓDULOS",20,true,DARK));
        String[] names={
            "MÓDULO 1 — BOLÕES DA ALDEOTA",
            "MÓDULO 2 — MEDIDOR DE REPETIDAS",
            "MÓDULO 3 — ACENDE E APAGA",
            "MÓDULO 4 — SEMELHANÇA DE RESULTADOS COM REPETIDAS",
            "MÓDULO 5 — TROCA DETALHADA RESULTADO × ESPELHO",
            "MÓDULO 6 — COMBINAÇÕES 5 / 6 / 7",
            "MÓDULO 7 — FALHA EM MOVIMENTO / PASSO DE FALHA",
            "MÓDULO 8 — FORÇA PREDOMINANTE DE FALHA"
        };
        for(int i=0;i<names.length;i++){final int id=i+1;Button b=button(names[i]);root.addView(b,match(dp(64)));b.setOnClickListener(v->openModule(id));}
        content=vbox();root.addView(content,new LinearLayout.LayoutParams(-1,-2));
        setContentView(sv);
    }

    boolean check(){if(model==null){toast("Carregue primeiro o TXT de resultados.");return false;}return true;}
    Spinner repeatSpinner(){
        Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"8 repetidas","9 repetidas","10 repetidas"}));s.setSelection(1);return s;
    }
    void openModule(int id){
        content.removeAllViews();
        content.addView(tv("MÓDULO "+id,19,true,DARK));
        if(id==1)module1();
        else if(id==2)module2();
        else if(id==3)module3();
        else if(id==4)module4();
        else if(id==5)module5();
        else if(id==6)module6();
        else if(id==7)module7();
        else module8();
    }

    void module1(){
        content.addView(tv("Percorre os bolões Aldeota organizados por 16, 17, 18, 19 e 20 dezenas; estuda padrão, evolução, ciclo, repetidas, falha do resultado e do espelho; gera 1 jogo.",14,false,TEXT));
        Button run=button("GERAR JOGO DOS BOLÕES ALDEOTA");content.addView(run,match(dp(60)));
        run.setOnClickListener(v->{if(!check())return;run.setEnabled(false);executor.submit(()->{try{
            CoreEngine.Candidate c=CoreEngine.module1(model,aldeota,this::progress);runOnUiThread(()->{show(c,"MÓDULO 1");run.setEnabled(true);});
        }catch(Exception e){error(run,e);}});});
    }
    void module2(){
        if(!check())return;CoreEngine.RepeatResult r=CoreEngine.module2(model);
        TextView t=tv(r.report,14,false,TEXT);t.setTextIsSelectable(true);content.addView(t);pdfButton("MÓDULO 2",r.report);
    }
    void module3(){
        if(!check())return;CoreEngine.RepeatResult rr=CoreEngine.module2(model);
        content.addView(tv("O Módulo 2 indicou "+rr.quantity+" repetidas. Aceite ou altere:",14,true,DARK));
        Spinner s=repeatSpinner();s.setSelection(rr.quantity-8);content.addView(s,match(dp(55)));
        Button run=button("EXECUTAR ACENDE E APAGA");content.addView(run,match(dp(60)));
        run.setOnClickListener(v->{int r=8+s.getSelectedItemPosition();run.setEnabled(false);executor.submit(()->{try{
            CoreEngine.Candidate c=CoreEngine.module3(model,r,this::progress);runOnUiThread(()->{show(c,"MÓDULO 3");run.setEnabled(true);});
        }catch(Exception e){error(run,e);}});});
    }
    void module4(){
        if(!check())return;StringBuilder cts=new StringBuilder("CONTAGEM DO UNIVERSO\n");
        for(CoreEngine.CountRow r:CoreEngine.module4Count(model))cts.append(r.repeats).append(" repetidas: dentro=").append(r.inside).append(" | fora=").append(r.outside).append("\n");
        content.addView(tv(cts.toString(),14,false,TEXT));
        Spinner rep=repeatSpinner();content.addView(rep,match(dp(55)));
        Spinner pat=new Spinner(this);pat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"DENTRO DO PADRÃO","FORA DO PADRÃO"}));content.addView(pat,match(dp(55)));
        EditText q=new EditText(this);q.setHint("Quantidade de jogos");q.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);content.addView(q,match(dp(55)));
        Button run=button("GERAR JOGOS");content.addView(run,match(dp(60)));
        run.setOnClickListener(v->{try{
            int qty=Integer.parseInt(q.getText().toString().trim()),r=8+rep.getSelectedItemPosition();boolean inside=pat.getSelectedItemPosition()==0;
            run.setEnabled(false);executor.submit(()->{try{
                List<CoreEngine.Candidate> list=CoreEngine.module4Generate(model,r,inside,qty,this::progress);
                runOnUiThread(()->{StringBuilder out=new StringBuilder(cts).append("\nGERADOS\n");int i=1;for(CoreEngine.Candidate x:list)out.append(i++).append(") ").append(CoreEngine.fmt(x.game)).append("\n");showText(out.toString(),"MÓDULO 4");run.setEnabled(true);});
            }catch(Exception e){error(run,e);}});
        }catch(Exception e){toast("Informe a quantidade.");}});
    }
    void module5(){
        if(!check())return;Spinner s=repeatSpinner();content.addView(s,match(dp(55)));Button run=button("ANALISAR TROCA E GERAR JOGO");content.addView(run,match(dp(60)));
        run.setOnClickListener(v->{int r=8+s.getSelectedItemPosition();run.setEnabled(false);executor.submit(()->{try{
            CoreEngine.Candidate c=CoreEngine.module5(model,r,this::progress);runOnUiThread(()->{show(c,"MÓDULO 5");run.setEnabled(true);});
        }catch(Exception e){error(run,e);}});});
    }
    void module6(){
        if(!check())return;content.addView(tv("10 repetidas → 5 por 5\n9 repetidas → 6 por 6\n8 repetidas → 7 por 7",14,true,DARK));
        Spinner s=repeatSpinner();content.addView(s,match(dp(55)));Button run=button("MINERAR BANCO 5/6/7");content.addView(run,match(dp(60)));
        run.setOnClickListener(v->{int r=8+s.getSelectedItemPosition();run.setEnabled(false);executor.submit(()->{try{
            CoreEngine.Candidate c=CoreEngine.module6(model,r,this::progress);runOnUiThread(()->{show(c,"MÓDULO 6");run.setEnabled(true);});
        }catch(Exception e){error(run,e);}});});
    }
    void module7(){
        if(!check())return;content.addView(tv("Procura a combinação de 10 que deu o primeiro passo de falha do penúltimo para o último e mede qual pedaço tende a continuar falhando.",14,false,TEXT));
        Button run=button("EXECUTAR PASSO DE FALHA");content.addView(run,match(dp(60)));
        run.setOnClickListener(v->{run.setEnabled(false);executor.submit(()->{try{
            CoreEngine.Candidate c=CoreEngine.module7(model,this::progress);runOnUiThread(()->{show(c,"MÓDULO 7");run.setEnabled(true);});
        }catch(Exception e){error(run,e);}});});
    }
    void module8(){
        if(!check())return;
        content.addView(tv("O motor pergunta e responde: QUANTAS dezenas do último resultado têm a força de falha predominante e QUAIS são. Depois mede o espelho na proporção complementar para fechar exatamente 10 falhas.",14,false,TEXT));
        Button run=button("DESCOBRIR FORÇA PREDOMINANTE E GERAR JOGO");content.addView(run,match(dp(64)));
        run.setOnClickListener(v->{run.setEnabled(false);executor.submit(()->{try{
            CoreEngine.Candidate c=CoreEngine.module8(model,this::progress);runOnUiThread(()->{show(c,"MÓDULO 8");run.setEnabled(true);});
        }catch(Exception e){error(run,e);}});});
    }

    void show(CoreEngine.Candidate c,String title){
        String text=title+"\n\nJOGO FINAL\n"+CoreEngine.fmt(c.game)+"\n\nSCORE "+String.format(Locale.US,"%.2f",c.score)+"\n\nJUSTIFICATIVA\n"+c.report+
            "\n\nFILTROS OBRIGATÓRIOS: jogo que já fez 15 pontos é descartado; somente 8, 9 ou 10 repetidas são aceitas.";
        showText(text,title);
    }
    void showText(String text,String title){
        TextView t=tv(text,14,false,TEXT);t.setTextIsSelectable(true);content.addView(t);
        pdfButton(title,text);
    }
    void pdfButton(String title,String text){Button b=button("GERAR PDF — JOGO + ANÁLISE + JUSTIFICATIVA");content.addView(b,match(dp(58)));b.setOnClickListener(v->pdf(title,text));}
    void progress(int pct,String msg){runOnUiThread(()->status.setText(msg+" — "+pct+"%"));}
    void error(Button b,Exception e){runOnUiThread(()->{status.setText("Erro: "+e.getMessage());toast(e.getMessage());b.setEnabled(true);});}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    void pick(int req){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/*");startActivityForResult(i,req);
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(res!=RESULT_OK||data==null||data.getData()==null)return;
        try(InputStream in=getContentResolver().openInputStream(data.getData())){
            if(req==10){history=new ArrayList<>(CoreEngine.parseHistory(in));model=new CoreEngine.Model(history);status.setText("TXT carregado: "+history.size()+" concursos.");}
            else {aldeota=new ArrayList<>(CoreEngine.parseAldeota(in));status.setText("Base Aldeota carregada: "+aldeota.size()+" grupos.");}
        }catch(Exception e){toast("Erro ao carregar: "+e.getMessage());}
    }

    void pdf(String title,String report){
        try{
            ArrayList<String> lines=wrap(report,84);PdfDocument doc=new PdfDocument();int at=0,pn=1;
            while(at<lines.size()){
                PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,pn++).create());
                Canvas c=page.getCanvas();Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(PURPLE);c.drawRect(0,0,595,86,p);p.setColor(WHITE);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(18);c.drawText("MINERADOR LOTOFÁCIL 8M",24,34,p);p.setTextSize(11);c.drawText(title,24,59,p);
                p.setColor(TEXT);p.setTypeface(Typeface.DEFAULT);p.setTextSize(9);int y=108;
                while(at<lines.size()&&y<810){c.drawText(lines.get(at++),24,y,p);y+=13;}
                doc.finishPage(page);
            }
            ContentValues v=new ContentValues();v.put(MediaStore.Downloads.DISPLAY_NAME,"MINERADOR_LOTOFACIL_8M_"+System.currentTimeMillis()+".pdf");v.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");v.put(MediaStore.Downloads.RELATIVE_PATH,"Download/MINERADOR_LOTOFACIL_8M");
            Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
            OutputStream os=getContentResolver().openOutputStream(uri);doc.writeTo(os);os.close();doc.close();toast("PDF salvo em Downloads/MINERADOR_LOTOFACIL_8M");
        }catch(Exception e){toast("Erro no PDF: "+e.getMessage());}
    }
    ArrayList<String> wrap(String text,int max){
        ArrayList<String> out=new ArrayList<>();
        for(String line:text.split("\\n",-1)){
            String s=line;if(s.isEmpty()){out.add("");continue;}
            while(s.length()>max){int x=s.lastIndexOf(' ',max);if(x<12)x=max;out.add(s.substring(0,x));s=s.substring(x).trim();}
            out.add(s);
        }
        return out;
    }
}
