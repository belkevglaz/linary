"""
Рисует иконку приложения: python3 packaging/icons/draw_icon.py
Пишет linary-<N>.png рядом со скриптом и app_icon.png (256) в ресурсы Compose.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageChops
import math

HERE = Path(__file__).resolve().parent
RESOURCES = HERE.parent.parent / "composeApp/src/commonMain/composeResources/drawable"
S=2048
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(3))
def hexc(h): h=h.lstrip('#'); return tuple(int(h[i:i+2],16) for i in (0,2,4))
img=Image.new('RGBA',(S,S),(0,0,0,0))
# background gradient rounded square
m=int(S*0.06); r=int(S*0.22)
grad=Image.new('RGBA',(S,S))
gd=ImageDraw.Draw(grad)
top,bot=hexc('#0097A7'),hexc('#00515C')
for y in range(S):
    gd.line([(0,y),(S,y)],fill=lerp(top,bot,y/S)+(255,))
mask=Image.new('L',(S,S),0)
ImageDraw.Draw(mask).rounded_rectangle([m,m,S-m,S-m],r,fill=255)
img.paste(grad,(0,0),mask)
# subtle top highlight
# page shadow
px0,py0,px1,py1=int(S*.25),int(S*.22),int(S*.75),int(S*.78)
pr=int(S*.05)
sh=Image.new('L',(S,S),0)
ImageDraw.Draw(sh).rounded_rectangle([px0,py0+int(S*.025),px1,py1+int(S*.025)],pr,fill=110)
sh=sh.filter(ImageFilter.GaussianBlur(S*.02))
img=Image.composite(Image.new('RGBA',(S,S),(0,40,45,255)),img,sh)
d=ImageDraw.Draw(img)
# page
d.rounded_rectangle([px0,py0,px1,py1],pr,fill=hexc('#FBFEFE'))
# header band
band=int(S*.15)
hb=Image.new('L',(S,S),0)
ImageDraw.Draw(hb).rounded_rectangle([px0,py0,px1,py1],pr,fill=255)
hb2=Image.new('L',(S,S),0); ImageDraw.Draw(hb2).rectangle([px0,py0,px1,py0+band],fill=255)
hb=ImageChops.multiply(hb,hb2)
img=Image.composite(Image.new('RGBA',(S,S),hexc('#4DD0E1')+(255,)),img,hb)
d=ImageDraw.Draw(img)
# binder rings
for cx in (S*.37,S*.63):
    rw,rh=S*.028,S*.085
    d.rounded_rectangle([cx-rw,py0-rh*0.55,cx+rw,py0+rh*0.55],int(rw),fill=hexc('#00363D'))
# text lines
lx0=px0+int(S*.06); ly=py0+band+int(S*.075)
for w in (0.38,0.32,0.36,0.22):
    d.rounded_rectangle([lx0,ly,lx0+int(S*w),ly+int(S*.034)],int(S*.017),fill=hexc('#B7D3D6'))
    ly+=int(S*.075)
# star
def star(cx,cy,R,rr):
    pts=[]
    for i in range(10):
        a=-math.pi/2+i*math.pi/5
        rad=R if i%2==0 else rr
        pts.append((cx+rad*math.cos(a),cy+rad*math.sin(a)))
    return pts
cx,cy=S*.715,S*.715
ss=Image.new('L',(S,S),0); ImageDraw.Draw(ss).polygon(star(cx,cy+S*.02,S*.16,S*.072),fill=120)
ss=ss.filter(ImageFilter.GaussianBlur(S*.018))
img=Image.composite(Image.new('RGBA',(S,S),(0,40,45,255)),img,ss)
d=ImageDraw.Draw(img)
st=Image.new('RGBA',(S,S),(0,0,0,0))
ImageDraw.Draw(st).polygon(star(cx,cy,S*.16,S*.072),fill=hexc('#FFC94D'))
# rounded star via blur+threshold
a=st.split()[3].filter(ImageFilter.GaussianBlur(S*.012)).point(lambda v:255 if v>128 else 0)
img=Image.composite(Image.new('RGBA',(S,S),hexc('#FFC94D')+(255,)),img,a)
inner=Image.new('L',(S,S),0); ImageDraw.Draw(inner).polygon(star(cx,cy-S*.01,S*.11,S*.05),fill=255)
inner=inner.filter(ImageFilter.GaussianBlur(S*.01)).point(lambda v:60 if v>128 else 0)
img=Image.composite(Image.new('RGBA',(S,S),(255,236,170,255)),img,inner)
for n in (16, 24, 32, 48, 64, 128, 256, 512):
    img.resize((n, n), Image.LANCZOS).save(HERE / f"linary-{n}.png")
img.resize((256, 256), Image.LANCZOS).save(RESOURCES / "app_icon.png")
