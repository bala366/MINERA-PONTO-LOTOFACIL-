package br.com.mineradorlotofacil.oito;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public final class CoreEngine {
    private CoreEngine(){}

    public static final int FULL=(1<<25)-1;

    public interface Progress { void update(int pct,String msg); }

    public static int maskOf(int[] a){
        int m=0; for(int n:a) if(n>=1&&n<=25) m|=1<<(n-1); return m;
    }
    public static int[] numsOf(int m){
        int[] a=new int[Integer.bitCount(m)]; int k=0;
        for(int n=1;n<=25;n++) if((m&(1<<(n-1)))!=0) a[k++]=n;
        return a;
    }
    public static String fmt(int m){
        int[] a=numsOf(m); StringBuilder s=new StringBuilder();
        for(int n:a){ if(s.length()>0)s.append(" "); s.append(String.format(Locale.US,"%02d",n)); }
        return s.toString();
    }

    public static List<int[]> parseHistory(InputStream in)throws IOException{
        ArrayList<int[]> out=new ArrayList<>();
        BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));
        String line;
        while((line=br.readLine())!=null){
            Matcher mt=Pattern.compile("\\d+").matcher(line.replace("\uFEFF",""));
            ArrayList<Integer> v=new ArrayList<>();
            while(mt.find()) try{v.add(Integer.parseInt(mt.group()));}catch(Exception ignored){}
            int[] chosen=null;
            // procura de trás para frente um bloco válido de 15 dezenas 1..25
            for(int st=Math.max(0,v.size()-15); st>=0 && chosen==null; st--){
                if(st+15>v.size()) continue;
                TreeSet<Integer> s=new TreeSet<>(); boolean ok=true;
                for(int j=0;j<15;j++){
                    int n=v.get(st+j);
                    if(n<1||n>25||!s.add(n)){ok=false;break;}
                }
                if(ok){
                    chosen=new int[15];int k=0;for(int n:s)chosen[k++]=n;
                }
            }
            if(chosen!=null) out.add(chosen);
        }
        if(out.size()<4) throw new IOException("TXT inválido ou com poucos concursos.");
        return out;
    }

    public static final class Model{
        public final int[] draw, fail;
        public final HashSet<Integer> winners=new HashSet<>();
        public Model(List<int[]> h){
            draw=new int[h.size()]; fail=new int[h.size()];
            for(int i=0;i<h.size();i++){
                draw[i]=maskOf(h.get(i));
                fail[i]=FULL^draw[i];
                winners.add(draw[i]);
            }
        }
        public int lastDraw(){return draw[draw.length-1];}
        public int lastMirror(){return fail[fail.length-1];}
        public boolean already15(int game){return winners.contains(game);}
    }

    public static final class Candidate{
        public final int game;
        public final double score;
        public final String report;
        public Candidate(int g,double s,String r){game=g;score=s;report=r;}
    }
    public static final class RepeatResult{
        public final int quantity, mask;
        public final String report;
        public RepeatResult(int q,int m,String r){quantity=q;mask=m;report=r;}
    }
    public static final class CountRow{
        public final int repeats, inside, outside;
        public CountRow(int r,int i,int o){repeats=r;inside=i;outside=o;}
    }

    // ---------- padrão ----------
    static final int BORDER=maskOf(new int[]{1,2,3,4,5,6,10,11,15,16,20,21,22,23,24,25});
    static int sum(int m){int s=0;for(int n:numsOf(m))s+=n;return s;}
    static int even(int m){int c=0;for(int n:numsOf(m))if(n%2==0)c++;return c;}
    static int ov(int a,int b){return Integer.bitCount(a&b);}
    public static boolean inPattern(int game){
        int e=even(game), b=ov(game,BORDER), s=sum(game);
        return e>=6&&e<=9 && b>=8&&b<=12 && s>=160&&s<=230;
    }

    static double numberHit(Model m,int n){
        int bit=1<<(n-1),all=0,r10=0,r30=0;
        for(int i=0;i<m.draw.length;i++) if((m.draw[i]&bit)!=0){
            all++; if(i>=m.draw.length-10)r10++; if(i>=m.draw.length-30)r30++;
        }
        return all*.10+r30*2+r10*5;
    }
    static double numberFail(Model m,int n){
        int bit=1<<(n-1),all=0,r10=0,r30=0;
        for(int i=0;i<m.fail.length;i++) if((m.fail[i]&bit)!=0){
            all++; if(i>=m.fail.length-10)r10++; if(i>=m.fail.length-30)r30++;
        }
        return all*.10+r30*2+r10*5;
    }
    static boolean validRepeatRange(Model m,int g){
        int r=ov(g,m.lastDraw());
        return r==8 || r==9 || r==10;
    }
    static boolean validFinalGame(Model m,int g){
        return !m.already15(g) && inPattern(g) && validRepeatRange(m,g);
    }
    static double gameScore(Model m,int g){
        if(!validFinalGame(m,g)) return -1e100;
        double s=40;
        for(int n:numsOf(g))s+=numberHit(m,n)*.04;
        int r=ov(g,m.lastDraw());
        if(r==8||r==9||r==10)s+=15;
        return s;
    }

    static void comb(int[] v,int k,int idx,int chosen,int mask,List<Integer> out){
        if(chosen==k){out.add(mask);return;}
        int need=k-chosen;
        for(int i=idx;i<=v.length-need;i++) comb(v,k,i+1,chosen+1,mask|(1<<(v[i]-1)),out);
    }
    static List<Integer> combos(int mask,int k){
        ArrayList<Integer> out=new ArrayList<>();
        if(k<0||k>Integer.bitCount(mask))return out;
        if(k==0){out.add(0);return out;}
        comb(numsOf(mask),k,0,0,0,out);return out;
    }

    // score de falha inteira + parcial + continuidade
    static double failStrength(Model m,int combo){
        int k=Integer.bitCount(combo), full=0, partial=0, streakPairs=0, prev=0;
        for(int f:m.fail){
            int x=ov(combo,f);
            partial+=x;
            int now=x==k?1:0;
            if(now==1)full++;
            if(now==1&&prev==1)streakPairs++;
            prev=now;
        }
        if(k==0)return 0;
        return full*k*14.0 + partial*1.25 + streakPairs*24.0;
    }
    static double hitStrength(Model m,int combo){
        int k=Integer.bitCount(combo), full=0, partial=0;
        for(int d:m.draw){
            int x=ov(combo,d); partial+=x; if(x==k)full++;
        }
        if(k==0)return 0;
        return full*k*12.0+partial*1.1;
    }
    static double transitionFailAgain(Model m,int combo){
        int k=Integer.bitCount(combo),op=0,score=0;
        for(int i=1;i<m.draw.length-1;i++){
            // combo estava falhando no i-1 e saiu/acesa em i
            if(ov(combo,m.fail[i-1])==k && ov(combo,m.draw[i])==k){
                op++; score+=ov(combo,m.fail[i+1]);
            }
        }
        return op==0?0:score*20.0/op;
    }
    static double transitionReturn(Model m,int combo){
        int k=Integer.bitCount(combo),op=0,score=0;
        for(int i=1;i<m.draw.length-1;i++){
            // combo vinha saindo e falhou em i
            if(ov(combo,m.draw[i-1])==k && ov(combo,m.fail[i])==k){
                op++; score+=ov(combo,m.draw[i+1]);
            }
        }
        return op==0?0:score*20.0/op;
    }

    static int bestSubset(Model m,int source,int k,boolean forFailure){
        double best=-1e100;int bm=0;
        for(int c:combos(source,k)){
            double s=forFailure?failStrength(m,c):hitStrength(m,c);
            if(s>best){best=s;bm=c;}
        }
        return bm;
    }

    // ================= MÓDULO 1 =================
    // Formato da base Aldeota: NOME;TAMANHO;DEZENAS
    public static final class AldeotaEntry{
        public final String name;public final int size,mask;
        public AldeotaEntry(String n,int s,int m){name=n;size=s;mask=m;}
    }
    public static List<AldeotaEntry> parseAldeota(InputStream in)throws IOException{
        ArrayList<AldeotaEntry> out=new ArrayList<>();
        BufferedReader br=new BufferedReader(new InputStreamReader(in,"UTF-8"));String line;
        while((line=br.readLine())!=null){
            line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;
            String[]p=line.split(";",3);if(p.length<3)continue;
            int size;try{size=Integer.parseInt(p[1].trim());}catch(Exception e){continue;}
            Matcher mt=Pattern.compile("\\d+").matcher(p[2]);TreeSet<Integer>s=new TreeSet<>();
            while(mt.find()){int n=Integer.parseInt(mt.group());if(n>=1&&n<=25)s.add(n);}
            if(s.size()!=size)continue;
            int[]a=new int[size];int k=0;for(int n:s)a[k++]=n;
            out.add(new AldeotaEntry(p[0].trim(),size,maskOf(a)));
        }
        return out;
    }
    public static Candidate module1(Model m,List<AldeotaEntry> bank,Progress p){
        if(bank==null||bank.isEmpty())throw new IllegalArgumentException("Carregue a base dos Bolões da Aldeota.");
        Candidate best=null;int ix=0;
        for(AldeotaEntry e:bank){
            if(e.size<15)continue;
            for(int g:combos(e.mask,15)){
                if(!validFinalGame(m,g))continue;
                double support=0;
                for(AldeotaEntry x:bank)support+=ov(g,x.mask);
                double sc=gameScore(m,g)+support*.15;
                if(best==null||sc>best.score)best=new Candidate(g,sc,
                    "Percorreu os Bolões Aldeota de 16/17/18/19/20, comparando padrão, evolução, ciclo, repetidas e forças de falha do resultado/espelho.");
            }
            ix++;if(p!=null)p.update(ix*95/Math.max(1,bank.size()),"Bolão "+ix+"/"+bank.size());
        }
        if(best==null)throw new IllegalStateException("Nenhum jogo Aldeota inédito dentro do padrão.");
        return best;
    }

    // ================= MÓDULO 2 =================
    public static RepeatResult module2(Model m){
        int[] hist=new int[16],r10=new int[16],r30=new int[16];
        for(int i=1;i<m.draw.length;i++){
            int r=ov(m.draw[i],m.draw[i-1]);hist[r]++;
            if(i>=m.draw.length-10)r10[r]++;
            if(i>=m.draw.length-30)r30[r]++;
        }
        int best=8;double bs=-1;
        StringBuilder rep=new StringBuilder("MÓDULO 2 — MEDIDOR DE REPETIDAS\n\n");
        for(int r=8;r<=10;r++){
            double sc=hist[r]+r30[r]*2.5+r10[r]*6;
            rep.append(r).append(" repetidas: histórico=").append(hist[r])
               .append(" últimos30=").append(r30[r]).append(" últimos10=").append(r10[r])
               .append(" score=").append(String.format(Locale.US,"%.2f",sc)).append("\n");
            if(sc>bs){bs=sc;best=r;}
        }
        Integer[] arr=Arrays.stream(numsOf(m.lastDraw())).boxed().toArray(Integer[]::new);
        Arrays.sort(arr,(a,b)->Double.compare(numberHit(m,b),numberHit(m,a)));
        int mask=0;for(int i=0;i<best;i++)mask|=1<<(arr[i]-1);
        rep.append("\nTENDÊNCIA: ").append(best).append(" repetidas\nQUAIS: ").append(fmt(mask));
        return new RepeatResult(best,mask,rep.toString());
    }

    // ================= MÓDULO 3 =================
    public static Candidate module3(Model m,int repeats,Progress p){
        int outN=15-repeats,inN=15-repeats;
        List<Integer> outs=combos(m.lastDraw(),outN), ins=combos(m.lastMirror(),inN);
        Candidate best=null;int i=0;
        for(int o:outs){
            double os=failStrength(m,o)+transitionFailAgain(m,o);
            for(int in:ins){
                int g=(m.lastDraw()&~o)|in;
                if(!validFinalGame(m,g))continue;
                double sc=os+hitStrength(m,in)+transitionReturn(m,in)+gameScore(m,g);
                if(best==null||sc>best.score)best=new Candidate(g,sc,
                    "ACENDE E APAGA: resultado que vinha falhando e saiu → mede retorno à falha; espelho que vinha saindo e falhou → mede retorno ao sorteio. Falha inteira e parcial.");
            }
            i++;if(p!=null)p.update(i*95/Math.max(1,outs.size()),"Resultado × espelho");
        }
        if(best==null)throw new IllegalStateException("Nenhum jogo inédito dentro do padrão.");
        return best;
    }

    // ================= MÓDULO 4 =================
    public static List<CountRow> module4Count(Model m){
        ArrayList<CountRow> rows=new ArrayList<>();
        for(int r=8;r<=10;r++){
            int inside=0,outside=0;
            for(int a:combos(m.lastDraw(),r))
                for(int b:combos(m.lastMirror(),15-r)){
                    int g=a|b;if(inPattern(g))inside++;else outside++;
                }
            rows.add(new CountRow(r,inside,outside));
        }
        return rows;
    }
    public static List<Candidate> module4Generate(Model m,int repeats,boolean inside,int qty,Progress p){
        PriorityQueue<Candidate> pq=new PriorityQueue<>(Comparator.comparingDouble(x->x.score));
        List<Integer>a=combos(m.lastDraw(),repeats),b=combos(m.lastMirror(),15-repeats);
        int ix=0;
        for(int x:a){
            for(int y:b){
                int g=x|y;if(inPattern(g)!=inside||m.already15(g)||!validRepeatRange(m,g))continue;
                Candidate c=new Candidate(g,gameScore(m,g),"Semelhança com o último resultado: "+repeats+" repetidas; "+(inside?"dentro":"fora")+" do padrão.");
                if(pq.size()<qty)pq.offer(c);else if(c.score>pq.peek().score){pq.poll();pq.offer(c);}
            }
            ix++;if(p!=null)p.update(ix*95/Math.max(1,a.size()),"Varredura universo");
        }
        ArrayList<Candidate> r=new ArrayList<>(pq);r.sort((x,y)->Double.compare(y.score,x.score));return r;
    }

    // ================= MÓDULO 5 =================
    public static Candidate module5(Model m,int repeats,Progress p){
        Candidate c=module3(m,repeats,p);
        return new Candidate(c.game,c.score,c.report+
            "\n\nMÓDULO 5 detalha explicitamente a troca: qual bloco sai do resultado, qual bloco entra do espelho, força inteira/parcial e justificativa.");
    }

    // ================= MÓDULO 6 =================
    public static Candidate module6(Model m,int repeats,Progress p){
        int k=15-repeats;
        if(k<5||k>7)throw new IllegalArgumentException("Módulo 6 trabalha com 8/9/10 repetidas.");
        List<Integer> outs=combos(m.lastDraw(),k),ins=combos(m.lastMirror(),k);
        outs.sort((a,b)->Double.compare(failStrength(m,b),failStrength(m,a)));
        ins.sort((a,b)->Double.compare(hitStrength(m,b),hitStrength(m,a)));
        int lo=Math.min(80,outs.size()),li=Math.min(80,ins.size());
        Candidate best=null;int bo=0,bi=0;
        for(int i=0;i<lo;i++){
            int o=outs.get(i);
            for(int j=0;j<li;j++){
                int in=ins.get(j),g=(m.lastDraw()&~o)|in;
                if(!validFinalGame(m,g))continue;
                double sc=failStrength(m,o)+hitStrength(m,in)+gameScore(m,g);
                if(best==null||sc>best.score){best=new Candidate(g,sc,"");bo=o;bi=in;}
            }
            if(p!=null)p.update(i*95/Math.max(1,lo),"Banco C("+k+")");
        }
        if(best==null)throw new IllegalStateException("Nenhum jogo válido.");
        return new Candidate(best.game,best.score,
            "MÓDULO 6 — "+repeats+" repetidas → troca "+k+" por "+k+
            "\nSAIR do resultado: "+fmt(bo)+"\nENTRAR do espelho: "+fmt(bi)+
            "\nForça falha saída="+String.format(Locale.US,"%.2f",failStrength(m,bo))+
            "\nForça presença entrada="+String.format(Locale.US,"%.2f",hitStrength(m,bi)));
    }

    // ================= MÓDULO 7 =================
    // "primeiro passo de falha": combinação de 10 cujo número de falhas cresceu do penúltimo p/ último
    public static Candidate module7(Model m,Progress p){
        if(m.draw.length<3)throw new IllegalArgumentException("Histórico insuficiente.");
        int pen=m.draw.length-2,last=m.draw.length-1;
        // Universo relevante de combinações de 10 = as falhas históricas observadas e seus vizinhos.
        HashSet<Integer> universe=new HashSet<>();
        for(int f:m.fail)universe.add(f);

        int best10=0;double bs=-1e100;int step=0;
        int ix=0;
        for(int c:universe){
            int a=ov(c,m.fail[pen]), b=ov(c,m.fail[last]);
            int delta=b-a;
            if(delta<=0)continue;
            double sc=failStrength(m,c)+delta*150;
            if(sc>bs){bs=sc;best10=c;step=delta;}
            ix++;if(p!=null)p.update(Math.min(70,ix*70/Math.max(1,universe.size())),"Ranking de passo de falha");
        }
        if(best10==0) best10=m.lastMirror();

        // mede qual pedaço da dez começou e tem maior continuidade de falha
        int bestPart=0;double bp=-1e100;
        for(int k=1;k<=Math.min(10,Integer.bitCount(best10));k++){
            for(int c:combos(best10,k)){
                double sc=failStrength(m,c)+transitionFailAgain(m,c);
                if(sc>bp){bp=sc;bestPart=c;}
            }
        }

        // estrutura maior dentro do resultado e menor dentro do espelho
        int fromResult=bestPart&m.lastDraw();
        int stayMirror=bestPart&m.lastMirror();

        // completa 10 falhas projetadas privilegiando força histórica
        int projected=fromResult|stayMirror;
        Integer[] all=Arrays.stream(numsOf(FULL^projected)).boxed().toArray(Integer[]::new);
        Arrays.sort(all,(a,b)->Double.compare(numberFail(m,b),numberFail(m,a)));
        for(int n:all){if(Integer.bitCount(projected)>=10)break;projected|=1<<(n-1);}
        while(Integer.bitCount(projected)>10){
            int rem=numsOf(projected)[0];projected&=~(1<<(rem-1));
        }
        int game=FULL^projected;
        if(!validFinalGame(m,game)){
            // fallback: módulo 3 com tendência do módulo2
            Candidate alt=module3(m,module2(m).quantity,p);
            game=alt.game;
        }
        if(!validRepeatRange(m,game)){
            Candidate alt=module3(m,module2(m).quantity,p);
            game=alt.game;
        }
        String r="MÓDULO 7 — FALHA EM MOVIMENTO / PASSO DE FALHA\n"+
            "Combinação de 10 sinalizada: "+fmt(best10)+"\n"+
            "Passo de falha do penúltimo para o último: +"+step+"\n"+
            "Subestrutura mais forte para continuar falhando: "+fmt(bestPart)+"\n"+
            "Parte no RESULTADO: "+fmt(fromResult)+"\n"+
            "Parte no ESPELHO: "+fmt(stayMirror)+"\n"+
            "Falha projetada final (10): "+fmt(FULL^game);
        return new Candidate(game,gameScore(m,game),r);
    }

    // ================= MÓDULO 8 =================
    // Descobre QUANTAS falhas do resultado e QUAL bloco, e completa as 10 falhas com o espelho.
    // Em Lotofácil uma falha inteira nunca pode ter tamanho >10, porque todo concurso deixa exatamente 10 dezenas fora.
    public static Candidate module8(Model m,Progress p){
        int result=m.lastDraw(), mirror=m.lastMirror();
        StringBuilder table=new StringBuilder("MÓDULO 8 — FORÇA PREDOMINANTE DE FALHA\n\n");
        int bestK=1,bestRes=0;double bestNorm=-1e100;

        // FILTRO OBRIGATÓRIO:
        // 5 falhas do resultado = 10 repetidas
        // 6 falhas do resultado = 9 repetidas
        // 7 falhas do resultado = 8 repetidas
        for(int k=5;k<=7;k++){
            int br=0;double bs=-1e100;
            for(int c:combos(result,k)){
                double raw=failStrength(m,c);
                // normaliza pelo tamanho para comparar "qual quantidade" sem favorecer só blocos pequenos
                double norm=raw/(k*k+1.0);
                if(norm>bs){bs=norm;br=c;}
            }
            table.append("Resultado — tamanho ").append(k).append(" — melhor bloco ")
                 .append(fmt(br)).append(" — força normalizada ")
                 .append(String.format(Locale.US,"%.2f",bs)).append("\n");
            if(bs>bestNorm){bestNorm=bs;bestK=k;bestRes=br;}
            if(p!=null)p.update((k-4)*45/3,"Medindo tamanho de falha no resultado — somente 8/9/10 repetidas");
        }

        int mirrorK=10-bestK;
        int bestMir=0;double bm=-1e100;
        for(int c:combos(mirror,mirrorK)){
            double sc=failStrength(m,c)/(mirrorK*mirrorK+1.0);
            if(sc>bm){bm=sc;bestMir=c;}
        }

        // top alternates in case the direct projected game already made 15
        List<Integer> resList=combos(result,bestK);
        List<Integer> mirList=combos(mirror,mirrorK);
        final int bestKFinal=bestK;
        final int mirrorKFinal=mirrorK;
        resList.sort((a,b)->Double.compare(failStrength(m,b)/(bestKFinal*bestKFinal+1.0),failStrength(m,a)/(bestKFinal*bestKFinal+1.0)));
        mirList.sort((a,b)->Double.compare(failStrength(m,b)/(mirrorKFinal*mirrorKFinal+1.0),failStrength(m,a)/(mirrorKFinal*mirrorKFinal+1.0)));

        Candidate best=null;int finalR=bestRes,finalM=bestMir;
        int lr=Math.min(100,resList.size()),lm=Math.min(100,mirList.size());
        for(int i=0;i<lr;i++){
            int rr=resList.get(i);
            for(int j=0;j<lm;j++){
                int mm=mirList.get(j);
                int failure=rr|mm;
                if(Integer.bitCount(failure)!=10)continue;
                int game=FULL^failure;
                if(!validFinalGame(m,game))continue;
                double sc=failStrength(m,rr)+failStrength(m,mm)+gameScore(m,game);
                if(best==null||sc>best.score){
                    best=new Candidate(game,sc,"");finalR=rr;finalM=mm;
                }
            }
            if(p!=null)p.update(45+i*50/Math.max(1,lr),"Cruzando falha resultado + espelho");
        }
        if(best==null)throw new IllegalStateException("Não achei projeção inédita dentro do padrão.");

        int repeats=ov(best.game,m.lastDraw());
        if(repeats<8 || repeats>10)
            throw new IllegalStateException("Filtro bloqueou jogo com "+repeats+" repetidas.");
        String report=table.toString()+
            "\nQUANTIDADE PREDOMINANTE DE FALHA DO RESULTADO: "+bestK+
            "\nBLOCO DO RESULTADO PARA FALHAR: "+fmt(finalR)+
            "\nQUANTIDADE DO ESPELHO QUE CONTINUA FALHANDO: "+mirrorK+
            "\nBLOCO DO ESPELHO PARA CONTINUAR FORA: "+fmt(finalM)+
            "\nTOTAL DE FALHAS PROJETADAS: 10"+
            "\nREPETIDAS RESULTANTES DO ÚLTIMO RESULTADO: "+repeats+
            "\nFILTRO OBRIGATÓRIO: somente 8, 9 ou 10 repetidas."+
            "\nJOGO PROJETADO: "+fmt(best.game)+
            "\n\nFiltro principal aplicado: jogo que já fez 15 pontos é descartado.";
        return new Candidate(best.game,best.score,report);
    }
}
