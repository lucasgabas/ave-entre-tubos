package com.exemplo.aveentretubos

import android.content.Context
import android.graphics.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.random.Random

class GameView(context: Context): View(context) {

    private val prefs=context.getSharedPreferences("progress",Context.MODE_PRIVATE)
    private val sound=ToneGenerator(AudioManager.STREAM_MUSIC,80)
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private val t=Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface=Typeface.create(Typeface.DEFAULT,Typeface.BOLD)
        textAlign=Paint.Align.CENTER
    }

    private enum class Screen { MENU,GAME,OVER,SHOP,MISSIONS,SETTINGS }
    private var screen=Screen.MENU
    private var difficulty=0
    private var score=0
    private var best=0
    private var coins=0
    private var xp=0
    private var level=1
    private var selected=0
    private var soundOn=true

    private var birdX=0f
    private var birdY=0f
    private var vy=0f
    private var last=System.nanoTime()

    private data class Pipe(var x:Float,var gap:Float,var passed:Boolean=false,var coin:Boolean=false)
    private val pipes=mutableListOf<Pipe>()

    // Skin: nome, preço em moedas, nível mínimo, premium, cor.
    private data class Skin(
        val name:String,val price:Int,val minLevel:Int,val premium:Boolean,val c:IntArray
    )
    private val skins=listOf(
        Skin("Pintinho",0,1,false,intArrayOf(250,190,45)),
        Skin("Azul",0,3,false,intArrayOf(70,165,245)),
        Skin("Rubi",0,7,false,intArrayOf(225,65,75)),
        Skin("Ninja",0,12,false,intArrayOf(45,45,55)),
        Skin("Arco-Íris",0,18,false,intArrayOf(175,80,220)),
        Skin("Dragão Verde",350,1,true,intArrayOf(65,205,90)),
        Skin("Dragão de Fogo",600,1,true,intArrayOf(240,85,35)),
        Skin("Dragão Sombrio",900,1,true,intArrayOf(65,45,100)),
        Skin("Fênix Dourada",1200,1,true,intArrayOf(245,170,35))
    )

    init {
        best=prefs.getInt("best",0)
        coins=prefs.getInt("coins",0)
        xp=prefs.getInt("xp",0)
        level=prefs.getInt("level",1)
        selected=prefs.getInt("selected",0)
        soundOn=prefs.getBoolean("sound",true)
        postInvalidateOnAnimation()
    }

    private fun xpForLevel(l:Int)=80+(l-1)*45

    private fun addXp(amount:Int) {
        xp+=amount
        while(xp>=xpForLevel(level)) {
            xp-=xpForLevel(level)
            level++
            coins+=50
            if(soundOn)sound.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,100)
        }
        save()
    }

    private fun save() {
        prefs.edit().putInt("best",best).putInt("coins",coins).putInt("xp",xp)
            .putInt("level",level).putInt("selected",selected).putBoolean("sound",soundOn).apply()
    }

    private fun owned(i:Int)=i==0 || prefs.getBoolean("skin_$i",false)

    private fun buyOrEquip(i:Int) {
        val s=skins[i]
        if(s.premium) {
            // Loja premium demonstrativa: compra com moedas do jogo.
            if(!owned(i) && coins>=s.price) {
                coins-=s.price
                prefs.edit().putBoolean("skin_$i",true).apply()
            }
        } else if(level>=s.minLevel) {
            prefs.edit().putBoolean("skin_$i",true).apply()
        }
        if(owned(i) && (!s.premium || prefs.getBoolean("skin_$i",false))) {
            selected=i
            save()
        }
    }

    private fun start() {
        screen=Screen.GAME
        score=0
        birdX=width*.27f
        birdY=height*.45f
        vy=-520f
        pipes.clear()
        pipes.add(Pipe(width+80f,randomGap()))
        pipes.add(Pipe(width+500f,randomGap()))
        last=System.nanoTime()
    }

    private fun randomGap()=height*.26f+Random.nextFloat()*(height*.68f-height*.26f)
    private fun speed()=when(difficulty){0->310f;1->360f;else->415f}
    private fun gap()=when(difficulty){0->225f;1->195f;else->170f}

    override fun onDraw(c:Canvas) {
        super.onDraw(c)
        val now=System.nanoTime()
        val dt=((now-last)/1e9f).coerceAtMost(.035f)
        last=now
        if(screen==Screen.GAME)update(dt)
        background(c)
        when(screen) {
            Screen.MENU->menu(c)
            Screen.GAME->game(c)
            Screen.OVER->{game(c);over(c)}
            Screen.SHOP->shop(c)
            Screen.MISSIONS->missions(c)
            Screen.SETTINGS->settings(c)
        }
        postInvalidateOnAnimation()
    }

    private fun update(dt:Float) {
        vy+=1500f*dt;birdY+=vy*dt
        pipes.forEach{it.x-=speed()*dt}
        if(pipes.maxByOrNull{it.x}?.x ?:0f < width-420f)
            pipes.add(Pipe(width+40f,randomGap()))
        if(pipes.isNotEmpty()&&pipes.first().x+85f<-40)pipes.removeAt(0)

        pipes.forEach{q->
            if(!q.passed&&q.x+85<birdX){
                q.passed=true;score++
                addXp(12)
                if(score%5==0){coins++;save()}
                if(soundOn)sound.startTone(ToneGenerator.TONE_PROP_BEEP,45)
            }
        }

        if(birdY<20||birdY>height-95)end()
        for(q in pipes) {
            val top=q.gap-gap()/2
            val bot=q.gap+gap()/2
            if(birdX+20>q.x&&birdX-20<q.x+85&&(birdY-20<top||birdY+20>bot)){end();return}
            if(!q.coin&&q.x+42<birdX+15&&q.x+42>birdX-35&&birdY>q.gap-30&&birdY<q.gap+30){
                q.coin=true;coins++;addXp(5);save()
                if(soundOn)sound.startTone(ToneGenerator.TONE_PROP_ACK,55)
            }
        }
    }

    private fun end() {
        best=max(best,score)
        addXp(score*3)
        save()
        screen=Screen.OVER
        if(soundOn)sound.startTone(ToneGenerator.TONE_PROP_NACK,120)
    }

    private fun background(c:Canvas) {
        c.drawColor(Color.rgb(105,195,238))
        p.color=Color.WHITE;p.alpha=160
        cloud(c,width*.15f,height*.17f,50f);cloud(c,width*.75f,height*.27f,40f)
        p.alpha=255
    }

    private fun game(c:Canvas) {
        pipes.forEach{pipe(c,it)}
        p.color=Color.rgb(225,190,90);c.drawRect(0f,height-75f,width.toFloat(),height.toFloat(),p)
        p.color=Color.rgb(85,175,70);c.drawRect(0f,height-83f,width.toFloat(),height-75f,p)
        bird(c,birdX,birdY,skins[selected].c)
        t.color=Color.WHITE;t.textSize=52f;t.setShadowLayer(4f,2f,2f,Color.BLACK)
        c.drawText("$score",width/2f,70f,t);t.clearShadowLayer()
        t.textSize=18f;c.drawText("🪙 $coins",width-58f,30f,t)
    }

    private fun menu(c:Canvas) {
        bird(c,width/2f,height*.19f,skins[selected].c)
        t.color=Color.WHITE;t.textSize=38f;t.setShadowLayer(4f,2f,3f,Color.DKGRAY)
        c.drawText("AVE ENTRE TUBOS",width/2f,height*.34f,t);t.clearShadowLayer()
        t.textSize=19f;c.drawText("NÍVEL $level  •  XP $xp/${xpForLevel(level)}",width/2f,height*.39f,t)
        button(c,height*.47f,"JOGAR")
        button(c,height*.55f,"LOJA DE AVES")
        button(c,height*.63f,"MISSÕES")
        button(c,height*.71f,"DIFICULDADE: "+when(difficulty){0->"FÁCIL";1->"NORMAL";else->"DIFÍCIL"})
        button(c,height*.79f,"CONFIGURAÇÕES")
        t.textSize=17f;c.drawText("🪙 $coins     🏆 Recorde $best",width/2f,height*.88f,t)
    }

    private fun over(c:Canvas) {
        overlay(c)
        t.color=Color.WHITE;t.textSize=36f;c.drawText("FIM DE JOGO",width/2f,height*.35f,t)
        t.textSize=22f;c.drawText("Pontos: $score   Recorde: $best",width/2f,height*.43f,t)
        button(c,height*.56f,"JOGAR NOVAMENTE")
        button(c,height*.65f,"MENU")
    }

    private fun shop(c:Canvas) {
        t.color=Color.WHITE;t.textSize=31f;c.drawText("LOJA DE AVES",width/2f,50f,t)
        t.textSize=18f;c.drawText("🪙 $coins  •  Nível $level",width/2f,80f,t)
        for(i in skins.indices){
            val y=height*.13f+i*height*.085f
            p.color=Color.argb(185,255,255,255);c.drawRoundRect(25f,y-31,width-25f,y+31,15f,15f,p)
            bird(c,width*.18f,y,skins[i].c)
            t.color=Color.DKGRAY;t.textSize=15f
            val s=skins[i]
            val label=when{
                selected==i->"${s.name} • EQUIPADA"
                owned(i)->"${s.name} • USAR"
                s.premium->"${s.name} • ${s.price} 🪙"
                else->"${s.name} • LIBERA NO NÍVEL ${s.minLevel}"
            }
            c.drawText(label,width*.64f,y+5,t)
        }
        t.color=Color.WHITE;t.textSize=14f;c.drawText("Skins premium usam moedas do jogo nesta versão.",width/2f,height*.94f,t)
    }

    private fun missions(c:Canvas) {
        t.color=Color.WHITE;t.textSize=31f;c.drawText("MISSÕES & CONQUISTAS",width/2f,55f,t)
        val missions=listOf(
            "Primeiro voo" to "Faça 1 ponto",
            "Piloto" to "Faça 10 pontos",
            "Veterano" to "Alcance 25 pontos",
            "Colecionador" to "Pegue 10 moedas",
            "Mestre dos tubos" to "Alcance 50 pontos",
            "Lendário" to "Chegue ao nível 20"
        )
        missions.forEachIndexed {i,(a,b)->
            val y=height*.15f+i*height*.11f
            val done=when(i){0->best>=1;1->best>=10;2->best>=25;3->coins>=10;4->best>=50;else->level>=20}
            p.color=if(done)Color.rgb(55,155,75) else Color.argb(185,255,255,255)
            c.drawRoundRect(30f,y-30,width-30f,y+30,14f,14f,p)
            t.color=if(done)Color.WHITE else Color.DKGRAY;t.textSize=17f
            c.drawText(if(done)"✓ $a" else a,width*.35f,y-2,t)
            t.textSize=13f;c.drawText(b,width*.35f,y+17,t)
            if(done){t.textSize=24f;c.drawText("🏆",width*.83f,y+8,t)}
        }
        t.color=Color.WHITE;t.textSize=15f;c.drawText("As recompensas de nível concedem 50 moedas automaticamente.",width/2f,height*.90f,t)
    }

    private fun settings(c:Canvas) {
        t.color=Color.WHITE;t.textSize=34f;c.drawText("CONFIGURAÇÕES",width/2f,90f,t)
        button(c,height*.30f,"SOM: "+if(soundOn)"LIGADO" else "DESLIGADO")
        button(c,height*.42f,"RESETAR PROGRESSO")
        t.textSize=15f;c.drawText("Versão 3.0 • progresso salvo no aparelho",width/2f,height*.55f,t)
        c.drawText("Toque em SOM para alternar.",width/2f,height*.60f,t)
    }

    private fun pipe(c:Canvas,q:Pipe) {
        val top=q.gap-gap()/2;val bot=q.gap+gap()/2
        p.color=Color.rgb(70,185,70);c.drawRect(q.x,0f,q.x+85,top,p);c.drawRect(q.x,bot,q.x+85,height-75f,p)
        p.color=Color.rgb(50,140,50);c.drawRect(q.x-7,top-28,q.x+92,top,p);c.drawRect(q.x-7,bot,q.x+92,bot+28,p)
        if(!q.coin){p.color=Color.rgb(255,210,35);c.drawCircle(q.x+42,q.gap,14f,p)}
    }

    private fun bird(c:Canvas,x:Float,y:Float,col:IntArray) {
        p.color=Color.rgb(col[0],col[1],col[2]);c.drawCircle(x,y,22f,p)
        p.color=Color.WHITE;c.drawCircle(x+8,y-7,7f,p)
        p.color=Color.BLACK;c.drawCircle(x+10,y-7,3f,p)
        p.color=Color.rgb(240,125,45)
        val b=Path();b.moveTo(x+18,y);b.lineTo(x+38,y+7);b.lineTo(x+18,y+13);b.close();c.drawPath(b,p)
        p.color=Color.rgb(235,165,35);c.drawOval(x-12,y+3,x+7,y+14,p)
        // pequeno detalhe para skins premium parecerem especiais
        if(col[0]<90&&col[2]>80){p.color=Color.rgb(120,230,140);c.drawCircle(x-17,y-12,5f,p)}
        if(col[0]>220&&col[1]<110){p.color=Color.rgb(255,190,50);c.drawCircle(x-18,y-10,5f,p)}
    }

    private fun cloud(c:Canvas,x:Float,y:Float,r:Float){
        c.drawCircle(x,y,r*.55f,p);c.drawCircle(x+r*.55f,y+5,r*.75f,p)
        c.drawCircle(x+r,y,r*.5f,p);c.drawRect(x-r*.5f,y,x+r*1.15f,y+r*.45f,p)
    }

    private fun overlay(c:Canvas){p.color=Color.argb(145,0,0,0);c.drawRect(0f,0f,width.toFloat(),height.toFloat(),p)}

    private fun button(c:Canvas,y:Float,label:String){
        p.color=Color.argb(215,35,125,70);c.drawRoundRect(45f,y-24,width-45f,y+24,16f,16f,p)
        t.color=Color.WHITE;t.textSize=19f;c.drawText(label,width/2f,y+7,t)
    }

    override fun onTouchEvent(e:MotionEvent):Boolean{
        if(e.action!=MotionEvent.ACTION_DOWN)return true
        val x=e.x;val y=e.y
        when(screen){
            Screen.MENU->when{
                y>height*.42&&y<height*.51->start()
                y>height*.51&&y<height*.59->screen=Screen.SHOP
                y>height*.59&&y<height*.67->screen=Screen.MISSIONS
                y>height*.67&&y<height*.75->{difficulty=(difficulty+1)%3}
                y>height*.75&&y<height*.84->screen=Screen.SETTINGS
            }
            Screen.GAME->vy=-520f
            Screen.OVER->when{
                y>height*.51&&y<height*.61->start()
                y>height*.61&&y<height*.72->screen=Screen.MENU
            }
            Screen.SHOP->{
                for(i in skins.indices){
                    val yy=height*.13f+i*height*.085f
                    if(y>yy-38&&y<yy+38){buyOrEquip(i);break}
                }
                if(y>height*.90)screen=Screen.MENU
            }
            Screen.MISSIONS->{if(y>height*.85)screen=Screen.MENU}
            Screen.SETTINGS->{
                if(y>height*.25&&y<height*.36){soundOn=!soundOn;save()}
                else if(y>height*.36&&y<height*.48){
                    prefs.edit().clear().apply();coins=0;xp=0;level=1;best=0;selected=0
                } else if(y>height*.48)screen=Screen.MENU
            }
        }
        invalidate();return true
    }
}
