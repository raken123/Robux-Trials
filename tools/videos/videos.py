"""The app's animated educational videos. Run render_all.py to produce the MP4 files."""
import math
import random

from engine import (CREAM, GOLD, GOLD_LIGHT, H, NAVY, NAVY_DARK, SKY, WHITE, W, Scene, Video, alpha, clamp, ease,
                    ease_out, lerp, mix, seg)


def sc(caption, draw, min_dur=5.0, narration=None, background=None):
    """Scene whose length fits the spoken caption (about 2.2 words per second)."""
    caption = caption.replace("ﷺ", "(frid vare med honom)")
    narration = narration.replace("ﷺ", "(frid vare med honom)") if narration else None
    words = len((narration or caption).split())
    return Scene(max(min_dur, words / 2.2 + 1.6), caption, draw, narration, background)


def title_card(c, text, sub=None, y=150):
    c.text(W / 2, y, text, 46, WHITE, "bold")
    if sub:
        c.text(W / 2, y + 52, sub, 26, GOLD)


def badge(c, x, y, n, a=1.0):
    c.circle(x, y, 26, fill=alpha(GOLD, a))
    c.text(x, y + 1, str(n), 26, alpha(NAVY_DARK, a), "bold")


# ---------------------------------------------------------------------------
# 1. Islams fem pelare
# ---------------------------------------------------------------------------

PILLARS = [
    ("Shahada", "شهادة", "Trosbekännelsen"),
    ("Salah", "صلاة", "Bönen"),
    ("Zakat", "زكاة", "Allmosan"),
    ("Sawm", "صوم", "Fastan"),
    ("Hajj", "حج", "Vallfärden"),
]


def temple(c, p_pillars, roof, highlight=-1, labels=1.0):
    base_y = 470
    x0, x1 = 280, 1000
    # steps
    c.rect(x0 - 40, base_y, x1 + 40, base_y + 22, fill=GOLD_LIGHT, radius=4)
    c.rect(x0 - 70, base_y + 22, x1 + 70, base_y + 44, fill=GOLD, radius=4)
    for i, (name, ar, sv) in enumerate(PILLARS):
        g = p_pillars[i]
        if g <= 0:
            continue
        cx = x0 + 50 + i * (x1 - x0 - 100) / 4
        top = base_y - 240 * g
        col = WHITE if i == highlight else CREAM
        c.rect(cx - 42, top, cx + 42, base_y, fill=col)
        for k in (-26, -13, 0, 13, 26):
            c.line([(cx + k, top + 18), (cx + k, base_y - 12)], (220, 214, 190), 3, round_caps=False)
        c.rect(cx - 52, top - 4, cx + 52, top + 14, fill=GOLD_LIGHT, radius=3)
        c.rect(cx - 52, base_y - 14, cx + 52, base_y, fill=GOLD_LIGHT, radius=3)
        if g > 0.95 and labels > 0:
            c.text(cx, base_y + 74, name, 24, alpha(WHITE if i != highlight else GOLD, labels), "bold")
            c.rect(cx - 36, top + 40, cx + 36, top + 84, fill=alpha(NAVY, labels), radius=8)
            c.text(cx, top + 60, ar, 28, alpha(GOLD_LIGHT, labels), "arbold")
    if roof > 0:
        drop = (1 - ease_out(roof)) * -200
        y = base_y - 244 + drop
        c.poly([(x0 - 70, y), (x1 + 70, y), ((x0 + x1) / 2, y - 110)], fill=alpha(GOLD, roof))
        c.rect(x0 - 80, y - 4, x1 + 80, y + 18, fill=alpha(GOLD_LIGHT, roof), radius=4)
        c.text((x0 + x1) / 2, y - 42, "الإسلام", 40, alpha(NAVY_DARK, roof), "arbold")


def pillars_scenes():
    scenes = [
        sc("Profeten Muhammad ﷺ sa: Islam är byggt på fem pelare.",
           lambda c, t, p: (title_card(c, "Islam vilar på fem pelare", "Bukhari och Muslim"),
                            temple(c, [0] * 5, 0))),
    ]
    texts = [
        "Den första pelaren är Shahada, trosbekännelsen: Det finns ingen gud utom Allah, och Muhammad är Allahs sändebud.",
        "Den andra pelaren är Salah: bönen fem gånger om dagen, vid Fajr, Dhuhr, Asr, Maghrib och Isha.",
        "Den tredje pelaren är Zakat: en årlig allmosa på två och en halv procent av sparat kapital till de behövande.",
        "Den fjärde pelaren är Sawm: att fasta från gryning till solnedgång under månaden Ramadan.",
        "Den femte pelaren är Hajj: vallfärden till Mecka, en gång i livet för den som har hälsa och råd.",
    ]
    for i, txt in enumerate(texts):
        def draw(c, t, p, i=i):
            grow = [1.0 if k < i else (ease_out(seg(p, 0.05, 0.35)) if k == i else 0) for k in range(5)]
            temple(c, grow, 0, highlight=i)
            name, ar, sv = PILLARS[i]
            c.text(W / 2, 120, f"{i + 1}. {name} – {sv}", 40, GOLD, "bold")
        scenes.append(sc(txt, draw, 7))
    scenes.append(sc(
        "Tillsammans bär pelarna upp hela byggnaden. Den som håller fast vid dem bygger sitt liv på en stadig grund.",
        lambda c, t, p: temple(c, [1] * 5, seg(p, 0.05, 0.4)), 7))
    return scenes


# ---------------------------------------------------------------------------
# 2. Salah – prayer positions with an articulated figure
# ---------------------------------------------------------------------------

L = dict(torso=118, neck=16, head=24, upper=66, fore=62, thigh=84, shin=84, foot=30)
GROUND = 560
MAT_X = 520


def deg(a):
    return math.radians(a)


def pose_standing(arms=(0, 0), head=0):
    return dict(hip=(MAT_X, GROUND - L["thigh"] - L["shin"] - 6), torso=0, head=head, upper=arms[0], fore=arms[1],
                thigh=0, shin=0, foot=90)


def pose_ruku():
    hip_y = GROUND - L["thigh"] - L["shin"] - 6
    return dict(hip=(MAT_X - 30, hip_y), torso=90, head=0, upper=-54, fore=-54, thigh=0, shin=0, foot=90)


def kneel(knee_x, thigh, torso, upper, fore, head=0):
    """Pose with knee on the ground and shin lying backwards (toes tucked)."""
    kx, ky = knee_x, GROUND - 10
    hx = kx - L["thigh"] * math.sin(deg(thigh))
    hy = ky - L["thigh"] * math.cos(deg(thigh))
    return dict(hip=(hx, hy), torso=torso, head=head, upper=upper, fore=fore, thigh=thigh, shin=-90, foot=-10)


def pose_sujud():
    return kneel(MAT_X + 40, 22, 100, 73, -38, head=50)


def pose_sitting(fore=72):
    return kneel(MAT_X + 60, 70, -4, 18, fore)


def joints(ps):
    hx, hy = ps["hip"]
    t = deg(ps["torso"])
    sx, sy = hx + L["torso"] * math.sin(t), hy - L["torso"] * math.cos(t)
    ht = deg(ps["torso"] + ps["head"])
    nx, ny = sx + L["neck"] * math.sin(ht), sy - L["neck"] * math.cos(ht)
    cx, cy = nx + L["head"] * math.sin(ht), ny - L["head"] * math.cos(ht)
    ex, ey = sx + L["upper"] * math.sin(deg(ps["upper"])), sy + L["upper"] * math.cos(deg(ps["upper"]))
    wx, wy = ex + L["fore"] * math.sin(deg(ps["fore"])), ey + L["fore"] * math.cos(deg(ps["fore"]))
    kx, ky = hx + L["thigh"] * math.sin(deg(ps["thigh"])), hy + L["thigh"] * math.cos(deg(ps["thigh"]))
    ax, ay = kx + L["shin"] * math.sin(deg(ps["shin"])), ky + L["shin"] * math.cos(deg(ps["shin"]))
    fx, fy = ax + L["foot"] * math.sin(deg(ps["foot"])), ay + L["foot"] * math.cos(deg(ps["foot"]))
    return dict(hip=(hx, hy), sh=(sx, sy), neck=(nx, ny), head=(cx, cy), el=(ex, ey), wr=(wx, wy), kn=(kx, ky),
                an=(ax, ay), ft=(fx, fy))


def blend(a, b, t):
    out = {}
    for k in a:
        if isinstance(a[k], tuple):
            out[k] = (lerp(a[k][0], b[k][0], t), lerp(a[k][1], b[k][1], t))
        else:
            out[k] = lerp(a[k], b[k], t)
    return out


def figure(c, ps, finger=False, look=0.0):
    j = joints(ps)
    far = (150, 138, 80)
    near = GOLD_LIGHT
    off = (-8, -3)

    def shift(pt):
        return (pt[0] + off[0], pt[1] + off[1])

    # far side limbs
    c.line([shift(j["sh"]), shift(j["el"]), shift(j["wr"])], far, 15)
    c.line([shift(j["hip"]), shift(j["kn"]), shift(j["an"]), shift(j["ft"])], far, 17)
    # body
    c.line([j["hip"], j["sh"]], near, 30)
    c.line([j["sh"], j["neck"]], near, 14)
    c.circle(*j["head"], L["head"], fill=near)
    # face direction marker (eye) – look shifts for the salam
    hx, hy = j["head"]
    ang = deg(ps["torso"] + ps["head"])
    ex = hx + 11 * math.cos(ang) * (1 - abs(look)) + look * 0
    ey = hy + 11 * math.sin(ang) - 4
    c.circle(ex, ey, 4, fill=NAVY_DARK)
    c.line([j["hip"], j["kn"], j["an"], j["ft"]], near, 18)
    c.line([j["sh"], j["el"], j["wr"]], near, 15)
    if finger:
        wx, wy = j["wr"]
        c.line([(wx, wy), (wx + 18, wy - 14)], near, 6)


def mat(c):
    c.rect(MAT_X - 230, GROUND - 6, MAT_X + 230, GROUND + 8, fill=(140, 30, 40), radius=4)
    c.rect(MAT_X - 230, GROUND - 6, MAT_X + 230, GROUND - 1, fill=GOLD)
    c.line([(80, GROUND + 9), (W - 80, GROUND + 9)], (60, 70, 150), 3, round_caps=False)
    # qibla direction hint
    c.text(MAT_X + 330, GROUND - 40, "Qibla →", 24, GOLD, "bold")
    c.rect(MAT_X + 300, GROUND - 130, MAT_X + 360, GROUND - 70, fill=(20, 20, 20))
    c.rect(MAT_X + 300, GROUND - 116, MAT_X + 360, GROUND - 108, fill=GOLD)


def side_panel(c, title, arabic=None, latin=None, step=None):
    x = 960
    if step:
        c.text(x, 150, step, 22, GOLD_LIGHT)
    c.text(x, 200, title, 40, WHITE, "bold")
    if arabic:
        c.text(x, 270, arabic, 42, GOLD, "ar")
    if latin:
        c.paragraph(x, 330, latin, 24, GOLD_LIGHT, 420)


ST = pose_standing()
TAKBIR = pose_standing(arms=(62, 172))
QIYAM = pose_standing(arms=(-12, 96))
RUKU = pose_ruku()
ITIDAL = pose_standing()
SUJUD = pose_sujud()
JALSA = pose_sitting()
TASH = pose_sitting(fore=96)


def anim(a, b, split=0.35, finger=False, look=None):
    def draw(c, t, p, a=a, b=b):
        mat(c)
        k = ease(seg(p, 0.05, split))
        figure(c, blend(a, b, k), finger=finger)
    return draw


def with_panel(drawer, *panel):
    def d(c, t, p):
        drawer(c, t, p)
        side_panel(c, *panel)
    return d


def salah_scenes():
    return [
        sc("Vänd dig mot Qibla, stå upprätt på bönemattan och gör din avsikt, niyya, i hjärtat.",
           with_panel(anim(ST, ST), "Niyya", "نية", "Avsikten görs i hjärtat", "Steg 1")),
        sc("Lyft händerna och säg Allahu Akbar för att påbörja bönen.",
           with_panel(anim(ST, TAKBIR), "Takbir", "الله أكبر", "Allahu Akbar – Allah är störst", "Steg 2")),
        sc("Lägg höger hand över vänster på bröstet och recitera Al-Fatiha och en kort sura.",
           with_panel(anim(TAKBIR, QIYAM), "Qiyam", "الفاتحة", "Recitera Al-Fatiha och en sura", "Steg 3")),
        sc("Säg Allahu Akbar och buga med rak rygg och händerna på knäna. Säg tre gånger Subhana Rabbiyal Adhim.",
           with_panel(anim(QIYAM, RUKU), "Ruku", "سبحان ربي العظيم",
                      "Subhana Rabbiyal Adhim – ära vare min Herre, den Väldige", "Steg 4"), 8),
        sc("Res dig upp och säg Sami Allahu liman hamidah, Rabbana wa lakal hamd.",
           with_panel(anim(RUKU, ITIDAL), "I'tidal", "ربنا ولك الحمد",
                      "Rabbana wa lakal hamd – vår Herre, Dig tillhör all lovprisning", "Steg 5")),
        sc("Säg Allahu Akbar och gå ner i sujud med panna, näsa, händer, knän och tår mot marken. "
           "Säg tre gånger Subhana Rabbiyal Ala.",
           with_panel(anim(ITIDAL, SUJUD, 0.45), "Sujud", "سبحان ربي الأعلى",
                      "Subhana Rabbiyal A'la – ära vare min Herre, den Högste", "Steg 6"), 9),
        sc("Sätt dig upp en kort stund och säg Rabbighfir li, och gå sedan ner i en andra sujud.",
           lambda c, t, p: (mat(c), figure(c, blend(SUJUD, JALSA, ease(seg(p, 0.05, 0.3)))
                                           if p < 0.6 else blend(JALSA, SUJUD, ease(seg(p, 0.65, 0.9)))),
                            side_panel(c, "Jalsa", "رب اغفر لي", "Rabbighfir li – Herre, förlåt mig", "Steg 7")), 7),
        sc("Res dig för nästa rakat och upprepa. Efter den andra rakaten sitter du och läser tashahhud.",
           with_panel(anim(SUJUD, TASH, 0.4, finger=True), "Tashahhud", "التحيات لله",
                      "At-tahiyyatu lillah – pekfingret lyfts vid trosbekännelsen", "Steg 8"), 7),
        sc("Avsluta genom att vända huvudet åt höger och sedan åt vänster och säga As-salamu alaykum wa rahmatullah.",
           with_panel(salam_draw, "Taslim", "السلام عليكم ورحمة الله",
                      "As-salamu alaykum wa rahmatullah", "Steg 9"), 8),
        sc("Fajr har två rakat, Dhuhr fyra, Asr fyra, Maghrib tre och Isha fyra.",
           rakat_table, 7),
    ]


def salam_draw(c, t, p):
    mat(c)
    figure(c, TASH)
    j = joints(TASH)
    hx, hy = j["head"]
    if p < 0.5:
        k = seg(p, 0.1, 0.3)
        c.text(hx, hy - 70, "→ höger", 26, alpha(GOLD, k), "bold")
    else:
        k = seg(p, 0.55, 0.75)
        c.text(hx, hy - 70, "vänster ←", 26, alpha(GOLD, k), "bold")


def rakat_table(c, t, p):
    rows = [("Fajr", 2), ("Dhuhr", 4), ("Asr", 4), ("Maghrib", 3), ("Isha", 4)]
    c.text(W / 2, 130, "Antal rakat (fard)", 40, WHITE, "bold")
    for i, (name, n) in enumerate(rows):
        k = seg(p, 0.05 + i * 0.1, 0.2 + i * 0.1)
        y = 210 + i * 66
        c.text(420, y, name, 32, alpha(WHITE, k), "bold", anchor="lm")
        for r in range(n):
            c.circle(700 + r * 60, y, 20 * k, fill=alpha(GOLD, k))
            c.text(700 + r * 60, y + 1, str(r + 1), 20, alpha(NAVY_DARK, k), "bold")


# ---------------------------------------------------------------------------
# 3. Wudu
# ---------------------------------------------------------------------------

WUDU = [
    ("🤲", "Avsikt och Bismillah", "Gör avsikten i hjärtat och säg Bismillah, i Allahs namn.", 0),
    ("🙌", "Händerna", "Tvätta händerna upp till handlederna, tre gånger. Börja med höger.", 3),
    ("👄", "Munnen", "Skölj munnen tre gånger.", 3),
    ("👃", "Näsan", "Dra upp vatten i näsan och blås ut, tre gånger.", 3),
    ("🙂", "Ansiktet", "Tvätta hela ansiktet från hårfästet till hakan och mellan öronen, tre gånger.", 3),
    ("💪", "Armarna", "Tvätta armarna upp till och med armbågarna, tre gånger. Först höger, sedan vänster.", 3),
    ("💆", "Huvudet", "Stryk de våta händerna över huvudet en gång, framifrån och bakåt och tillbaka.", 1),
    ("👂", "Öronen", "Torka öronen, insidan med pekfingrarna och baksidan med tummarna.", 1),
    ("🦶", "Fötterna", "Tvätta fötterna upp till och med anklarna, tre gånger. Börja med höger fot.", 3),
]


def water_drops(c, t, x, y, n=7):
    rnd = random.Random(1)
    for i in range(n):
        ph = (t * 0.9 + rnd.random()) % 1.0
        dx = rnd.uniform(-90, 90)
        dy = -150 + ph * 260
        a = 1 - abs(ph - 0.5) * 2
        c.ellipse(x + dx - 7, y + dy - 11, x + dx + 7, y + dy + 11, fill=alpha((130, 190, 255), a))


def wudu_step(i):
    em, name, txt, times = WUDU[i]

    def draw(c, t, p):
        # progress dots
        for k in range(len(WUDU)):
            x = W / 2 + (k - (len(WUDU) - 1) / 2) * 56
            done = k < i
            cur = k == i
            c.circle(x, 112, 18 if cur else 13, fill=GOLD if (done or cur) else (60, 70, 150))
            c.text(x, 113, str(k + 1), 16 if cur else 13, NAVY_DARK if (done or cur) else WHITE, "bold")
        cx, cy = 430, 360
        c.circle(cx, cy, 150, fill=(35, 48, 150))
        c.circle(cx, cy, 150, outline=GOLD, width=4)
        bob = math.sin(t * 2.4) * 6
        c.emoji(cx, cy + bob, em, 150)
        if times:
            water_drops(c, t, cx, cy)
        c.text(800, 280, name, c.fit(name, 52, 440, "bold"), WHITE, "bold", anchor="lm")
        if times:
            n = min(times, 1 + int(clamp(p / 0.85) * times))
            c.text(800, 350, f"{n} × av {times}", 34, GOLD, "bold", anchor="lm")
            for r in range(times):
                c.circle(820 + r * 50, 410, 15, fill=GOLD if r < n else (60, 70, 150))
        elif i > 0:
            c.text(800, 350, "en gång", 34, GOLD, "bold", anchor="lm")
    return sc(txt, draw, 6)


def wudu_scenes():
    intro = sc("Wudu, den rituella tvagningen, behövs före bönen. Så här går den till steg för steg.",
               lambda c, t, p: (c.emoji(W / 2, 300 + math.sin(t * 2) * 8, "💧", 160),
                                c.text(W / 2, 470, "Tvagningen före bönen", 40, WHITE, "bold")), 6)
    end = sc("Avsluta med trosbekännelsen: Ashhadu an la ilaha illa Allah, wa ashhadu anna Muhammadan abduhu wa rasuluh.",
             lambda c, t, p: (c.text(W / 2, 230, "أشهد أن لا إله إلا الله", 54, GOLD, "ar"),
                              c.text(W / 2, 320, "وأشهد أن محمدا عبده ورسوله", 54, GOLD, "ar"),
                              c.text(W / 2, 420, "Wudu är klar – nu är du redo för bönen", 32, WHITE, "bold")), 8)
    return [intro] + [wudu_step(i) for i in range(len(WUDU))] + [end]


# ---------------------------------------------------------------------------
# 4. Tasbih after the prayer
# ---------------------------------------------------------------------------

DHIKR = [
    ("سبحان الله", "SubhanAllah", "Ära vare Allah", 33),
    ("الحمد لله", "Alhamdulillah", "All lovprisning tillhör Allah", 33),
    ("الله أكبر", "Allahu Akbar", "Allah är störst", 33),
]


def beads(c, t, count, total=33):
    cx, cy, r = 380, 380, 190
    k = count
    for i in range(total):
        a = math.radians(-90 + 360 * i / total)
        x, y = cx + r * math.cos(a), cy + r * math.sin(a)
        c.circle(x, y, 13, fill=GOLD if i < k else (70, 82, 170))
    c.circle(cx, cy - r - 30, 16, fill=GOLD_LIGHT)
    c.text(cx, cy, str(k), 90, WHITE, "bold")
    c.text(cx, cy + 70, f"av {total}", 26, GOLD_LIGHT)


def tasbih_scenes():
    scenes = [sc("Efter varje obligatorisk bön är det sunna att prisa Allah med tasbih.",
                 lambda c, t, p: (beads(c, t, 0), c.text(900, 330, "Tasbih", 56, WHITE, "bold"),
                                  c.text(900, 400, "efter bönen", 30, GOLD)), 6)]
    for ar, lat, sv, n in DHIKR:
        def draw(c, t, p, ar=ar, lat=lat, sv=sv, n=n):
            count = min(n, int(seg(p, 0.08, 0.9) * n + 0.0001))
            beads(c, t, count, n)
            c.text(900, 270, ar, 64, GOLD, "ar")
            c.text(900, 350, lat, 40, WHITE, "bold")
            c.text(900, 400, sv, 26, GOLD_LIGHT)
        scenes.append(sc(f"Säg {lat}, {sv[0].lower() + sv[1:]}, trettiotre gånger.", draw, 8))
    scenes.append(sc(
        "Fullborda hundra med: La ilaha illa Allahu wahdahu la sharika lah, lahul mulku wa lahul hamd, "
        "wa huwa ala kulli shayin qadir. Den som säger detta får sina synder förlåtna, även om de är som havets skum.",
        lambda c, t, p: (c.paragraph(W / 2, 250, "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد، وهو على كل شيء قدير",
                                     46, GOLD, 1000, "ar"),
                         c.text(W / 2, 420, "Muslim 597", 24, GOLD_LIGHT)), 12))
    return scenes


# ---------------------------------------------------------------------------
# 5. Prayer times and the sun
# ---------------------------------------------------------------------------

def sky_color(h):
    """Sky colour for a sun height h in [-1, 1]."""
    night = (8, 12, 45)
    dawn = (230, 140, 90)
    day = (110, 170, 235)
    if h < -0.35:
        return night
    if h < 0:
        return mix(night, dawn, (h + 0.35) / 0.35)
    if h < 0.25:
        return mix(dawn, day, h / 0.25)
    return day


def landscape(c, ang, label=None, pole=False, hanafi=False):
    """ang: sun angle along its path in degrees, 0 = east horizon, 180 = west horizon."""
    h = math.sin(math.radians(ang))
    top = sky_color(h)
    horizon_y = 520
    c.rect(0, 0, W, horizon_y, fill=top)
    for k in range(6):  # soft glow near horizon
        c.rect(0, horizon_y - 60 + k * 10, W, horizon_y, fill=alpha(mix(top, (250, 200, 140), 0.5), 0.06))
    if h < 0.05:
        rnd = random.Random(3)
        for _ in range(60):
            x, y = rnd.uniform(0, W), rnd.uniform(80, 400)
            c.circle(x, y, rnd.uniform(1, 2.6), fill=alpha(WHITE, clamp(-h * 3)))
    cx, cy, R = W / 2, horizon_y, 430
    sx = cx - R * math.cos(math.radians(ang))
    sy = cy - 330 * math.sin(math.radians(ang))
    c.arc(cx, cy, R, 180, 360, alpha(WHITE, 0.15), 2)
    if sy < horizon_y + 30:
        for k in range(4):
            c.circle(sx, sy, 44 + k * 12, fill=(255, 220, 120, 30))
        c.circle(sx, sy, 40, fill=(255, 214, 90))
    # ground and mosque
    c.rect(0, horizon_y, W, H, fill=(30, 45, 40) if h > 0 else (12, 18, 22))
    mx = 640
    gc = (20, 28, 70)
    c.rect(mx - 120, horizon_y - 90, mx + 120, horizon_y, fill=gc)
    c.ellipse(mx - 80, horizon_y - 170, mx + 80, horizon_y - 30, fill=gc)
    c.rect(mx - 4, horizon_y - 200, mx + 4, horizon_y - 160, fill=gc)
    c.rect(mx + 150, horizon_y - 230, mx + 178, horizon_y, fill=gc)
    c.poly([(mx + 146, horizon_y - 230), (mx + 182, horizon_y - 230), (mx + 164, horizon_y - 270)], fill=gc)
    if h < 0:
        for wx in (-60, 0, 60):
            c.rect(mx + wx - 10, horizon_y - 60, mx + wx + 10, horizon_y - 25, fill=(255, 210, 120), radius=6)
    if pole and h > 0:
        px = 300
        c.line([(px, horizon_y), (px, horizon_y - 100)], WHITE, 8)
        shadow = min(900, 100 / max(math.tan(math.radians(ang if ang < 90 else 180 - ang)), 0.05))
        direction = -1 if ang < 90 else 1
        c.line([(px, horizon_y + 6), (px + direction * shadow, horizon_y + 6)], (0, 0, 0, 160), 10)
        c.line([(px, horizon_y + 22), (px + direction * 100, horizon_y + 22)], GOLD, 4)
        c.text(px + direction * 100 + 10, horizon_y + 22, "1×", 20, GOLD, "bold", anchor="lm")
        if hanafi:
            c.line([(px, horizon_y + 38), (px + direction * 200, horizon_y + 38)], GOLD_LIGHT, 4)
            c.text(px + direction * 200 + 10, horizon_y + 38, "2× (Hanafi)", 20, GOLD_LIGHT, "bold", anchor="lm")
    if label:
        c.rect(40, 100, 420, 200, fill=(10, 13, 50, 190), radius=16)
        c.text(70, 135, label[0], 40, GOLD, "bold", anchor="lm")
        c.text(70, 178, label[1], 24, WHITE, anchor="lm")


def sun_scenes():
    def move(a0, a1, label=None, pole=False, hanafi=False):
        return lambda c, t, p: landscape(c, lerp(a0, a1, ease(seg(p, 0.0, 0.7))), label, pole, hanafi)
    return [
        sc("Bönetiderna följer solens gång över himlen. Därför ändras de under året, särskilt här i Sverige.",
           move(-40, 200), 7),
        sc("Fajr börjar när gryningsljuset syns vid horisonten, en god stund innan solen går upp.",
           move(-30, -16, ("Fajr", "Gryningsljuset")), 7),
        sc("Soluppgången avslutar tiden för Fajr. Då ber man inte.",
           move(-16, 6, ("Shuruq", "Soluppgång")), 6),
        sc("Dhuhr börjar strax efter att solen passerat sin högsta punkt mitt på dagen.",
           move(60, 92, ("Dhuhr", "Efter middagshöjden")), 7),
        sc("Asr börjar när ett föremåls skugga är lika lång som föremålet självt, enligt Hanafi när den är dubbelt så lång.",
           move(120, 140, ("Asr", "Skuggan växer"), pole=True, hanafi=True), 9),
        sc("Maghrib börjar direkt när solen har gått ner.",
           move(165, 185, ("Maghrib", "Solnedgång")), 6),
        sc("Isha börjar när kvällsrodnaden har försvunnit och det har blivit mörkt.",
           move(188, 215, ("Isha", "Natten")), 6),
        sc("I appen räknas tiderna ut för just din plats, och telefonen kan väcka dig till Fajr.",
           move(215, 330, ("Salah", "I appen")), 7),
    ]


# ---------------------------------------------------------------------------
# 6. Hajj
# ---------------------------------------------------------------------------

PLACES = {"Mecka": (240, 330), "Mina": (520, 300), "Muzdalifah": (760, 380), "Arafat": (1030, 330)}


def kaaba(c, x, y, s=60):
    c.rect(x - s / 2, y - s / 2, x + s / 2, y + s / 2, fill=(15, 15, 15))
    c.rect(x - s / 2, y - s / 2 + s * 0.22, x + s / 2, y - s / 2 + s * 0.32, fill=GOLD)


def hajj_map(c, pilgrim=None, highlight=None, trail=None):
    c.rect(80, 150, 1200, 540, fill=(214, 190, 140), radius=30)
    rnd = random.Random(5)
    for _ in range(30):
        x, y = rnd.uniform(120, 1160), rnd.uniform(180, 520)
        c.ellipse(x - 40, y - 8, x + 40, y + 8, fill=(200, 175, 125))
    road = [PLACES["Mecka"], PLACES["Mina"], PLACES["Muzdalifah"], PLACES["Arafat"]]
    c.line(road, (170, 140, 95), 8)
    for name, (x, y) in PLACES.items():
        hl = name == highlight
        if name == "Mecka":
            kaaba(c, x, y, 54)
        elif name == "Mina":
            for k in range(5):
                tx = x - 50 + k * 25
                c.poly([(tx - 12, y + 10), (tx + 12, y + 10), (tx, y - 14)], fill=WHITE)
        elif name == "Arafat":
            c.poly([(x - 60, y + 20), (x + 60, y + 20), (x, y - 40)], fill=(160, 120, 80))
        else:
            c.circle(x, y, 10, fill=(150, 120, 80))
        c.rect(x - 75, y + 34, x + 75, y + 70, fill=GOLD if hl else (60, 50, 40, 200), radius=12)
        c.text(x, y + 52, name, 22, NAVY_DARK if hl else WHITE, "bold")
    if pilgrim:
        c.circle(pilgrim[0], pilgrim[1] - 30, 14, fill=WHITE, outline=NAVY_DARK, width=3)


def along(path, t):
    if t <= 0:
        return path[0]
    lens = [math.dist(a, b) for a, b in zip(path, path[1:])]
    total = sum(lens)
    d = t * total
    for (a, b), l in zip(zip(path, path[1:]), lens):
        if d <= l:
            k = d / l
            return (lerp(a[0], b[0], k), lerp(a[1], b[1], k))
        d -= l
    return path[-1]


def travel(names, label, highlight=None):
    path = [PLACES[n] for n in names]

    def draw(c, t, p):
        pos = along(path, ease(seg(p, 0.1, 0.8)))
        hajj_map(c, pos, highlight or names[-1])
        c.text(W / 2, 110, label, 34, GOLD, "bold")
    return draw


def tawaf(c, t, p, rounds=7, label="Tawaf – sju varv"):
    cx, cy = W / 2, 340
    c.circle(cx, cy, 210, fill=(225, 225, 220))
    c.circle(cx, cy, 210, outline=GOLD, width=3)
    kaaba(c, cx, cy, 90)
    done = seg(p, 0.05, 0.92) * rounds
    for k in range(40):
        ang = -done * 2 * math.pi + k * 2 * math.pi / 40
        r = 130 + (k % 3) * 25
        c.circle(cx + r * math.cos(ang), cy + r * math.sin(ang), 7, fill=(255, 255, 255), outline=(150, 150, 150), width=1)
    c.text(cx + 360, 260, label, 32, WHITE, "bold")
    c.text(cx + 360, 320, f"Varv {min(rounds, int(done) + 1)} av {rounds}", 30, GOLD, "bold")
    c.text(cx + 360, 370, "moturs, Kaba till vänster", 22, GOLD_LIGHT)


def sai(c, t, p):
    a, b = (330, 330), (950, 330)
    c.rect(250, 380, 1030, 400, fill=(225, 225, 220), radius=8)
    for x, name in ((a[0], "Safa"), (b[0], "Marwa")):
        c.poly([(x - 70, 380), (x + 70, 380), (x, 290)], fill=(170, 140, 100))
        c.text(x, 430, name, 28, WHITE, "bold")
    k = seg(p, 0.05, 0.92) * 7
    lap = int(min(k, 6.999))
    f = k - lap
    x = lerp(a[0], b[0], f) if lap % 2 == 0 else lerp(b[0], a[0], f)
    c.circle(x, 355, 14, fill=WHITE, outline=NAVY_DARK, width=3)
    c.text(W / 2, 200, "Sa'i mellan Safa och Marwa", 36, WHITE, "bold")
    c.text(W / 2, 250, f"Sträcka {lap + 1} av 7", 28, GOLD, "bold")


def ihram(c, t, p):
    c.emoji(420, 340 + math.sin(t * 2) * 6, "🕋", 170)
    c.text(800, 280, "Ihram", 56, WHITE, "bold", anchor="lm")
    c.text(800, 350, "لبيك اللهم لبيك", 46, GOLD, "ar", anchor="lm")
    c.text(800, 410, "Labbayk Allahumma labbayk", 26, GOLD_LIGHT, anchor="lm")


def jamarat(c, t, p, three=False):
    xs = [420, 640, 860] if three else [640]
    c.text(W / 2, 130, "Stenkastning vid Jamarat" if three else "Jamarat al-Aqaba", 36, WHITE, "bold")
    for x in xs:
        c.rect(x - 22, 230, x + 22, 470, fill=(200, 195, 185), radius=6)
    n = int(seg(p, 0.1, 0.9) * 7)
    for i in range(7):
        k = i < n
        c.circle(560 + i * 27, 520, 8, fill=GOLD if k else (90, 100, 170))
    c.text(W / 2, 560, f"{n} av 7 småstenar" + (" vid varje pelare" if three else ""), 24, GOLD_LIGHT)
    if n < 7:
        ph = (seg(p, 0.1, 0.9) * 7) % 1
        x0, y0 = 300, 520
        tx = xs[min(len(xs) - 1, n % len(xs))] if three else xs[0]
        c.circle(lerp(x0, tx, ph), lerp(y0, 330, ph) - 120 * math.sin(math.pi * ph), 8, fill=GOLD)


def hajj_scenes():
    return [
        sc("Hajj sker i månaden Dhul-Hijja. Pilgrimen går in i ihram, ett tillstånd av helgd, och säger talbiya: "
           "Labbayk Allahumma labbayk.", ihram, 9),
        sc("I Mecka gör pilgrimen tawaf: sju varv runt Kaba, moturs.", tawaf, 9),
        sc("Sedan går pilgrimen sju gånger mellan kullarna Safa och Marwa, till minne av Hajar.", sai, 9),
        sc("Den åttonde dagen beger sig pilgrimerna till tältstaden Mina.", travel(["Mecka", "Mina"], "8 Dhul-Hijja"), 6),
        sc("Den nionde dagen står pilgrimerna på Arafat och ber till Allah fram till solnedgången. Detta är Hajjens viktigaste del.",
           travel(["Mina", "Muzdalifah", "Arafat"], "9 Dhul-Hijja – Arafat"), 9),
        sc("Efter solnedgången går man till Muzdalifah, ber och övernattar under bar himmel och samlar småstenar.",
           travel(["Arafat", "Muzdalifah"], "Natten i Muzdalifah"), 8),
        sc("På Eid-dagen kastar man sju stenar på Jamarat al-Aqaba, offrar ett djur och rakar eller klipper håret.",
           jamarat, 9),
        sc("Därefter återvänder man till Mecka för tawaf al-ifada.",
           lambda c, t, p: tawaf(c, t, p, label="Tawaf al-ifada"), 6),
        sc("Under tashriq-dagarna i Mina kastar man sten på alla tre pelarna varje dag.",
           lambda c, t, p: jamarat(c, t, p, three=True), 7),
        sc("Innan man lämnar Mecka gör man en avskedstawaf. Må Allah acceptera din Hajj.",
           lambda c, t, p: tawaf(c, t, p, label="Avskedstawaf"), 7),
    ]


# ---------------------------------------------------------------------------
# 7. Ramadan
# ---------------------------------------------------------------------------

def moon(c, x, y, r, phase, color=GOLD_LIGHT, bg=NAVY_DARK):
    """phase 0 = new moon (thin crescent), 1 = full."""
    c.circle(x, y, r, fill=color)
    off = r * 2 * clamp(phase)
    c.circle(x - r * 0.35 - off, y - r * 0.1, r * 0.95, fill=bg)


def night_sky(c, t, density=50):
    c.rect(0, 70, W, H, fill=(8, 12, 45))
    rnd = random.Random(9)
    for _ in range(density):
        x, y = rnd.uniform(0, W), rnd.uniform(80, 560)
        tw = 0.5 + 0.5 * math.sin(t * rnd.uniform(1, 3) + rnd.uniform(0, 6))
        c.circle(x, y, rnd.uniform(1, 2.5), fill=alpha(WHITE, 0.4 + 0.6 * tw))


def ramadan_scenes():
    def crescent(c, t, p):
        night_sky(c, t)
        c.text(W / 2, 140, "Nymånen siktas", 40, WHITE, "bold")
        moon(c, W / 2 + 40, 330, 100, 0.0, bg=(8, 12, 45))
        c.emoji(W / 2 - 220, 470, "🔭", 90)

    def suhoor(c, t, p):
        night_sky(c, t, 30)
        c.rect(0, 520, W, 720, fill=(15, 20, 30))
        c.rect(0, 500, W, 520, fill=alpha((230, 140, 90), 0.5 * seg(p, 0.3, 1)))
        c.emoji(450, 360, "🍽️", 140)
        c.emoji(650, 380, "🥛", 90)
        c.text(900, 300, "Suhur", 52, WHITE, "bold")
        c.text(900, 360, "före Fajr", 30, GOLD)

    def fasting(c, t, p):
        landscape(c, lerp(10, 170, ease(p)))
        c.rect(380, 110, 900, 190, fill=(10, 13, 50, 200), radius=16)
        c.text(640, 150, "Ingen mat eller dryck under dagen", 30, WHITE, "bold")

    def iftar(c, t, p):
        landscape(c, 182)
        c.rect(330, 190, 950, 330, fill=(10, 13, 50, 200), radius=20)
        c.emoji(460, 260, "🌴", 90)
        c.emoji(640, 260, "💧", 80)
        c.emoji(820, 260, "🍲", 90)
        c.rect(430, 110, 850, 170, fill=(10, 13, 50, 200), radius=16)
        c.text(640, 140, "Iftar vid Maghrib", 40, GOLD, "bold")

    def tarawih(c, t, p):
        landscape(c, 230)
        c.text(640, 150, "Tarawih", 48, WHITE, "bold")  # night sky keeps contrast
        c.text(640, 205, "nattbön i moskén", 28, GOLD)

    def qadr(c, t, p):
        night_sky(c, t, 160)
        c.text(W / 2, 260, "ليلة القدر", 90, GOLD, "arbold")
        c.text(W / 2, 380, "Bättre än tusen månader", 36, WHITE, "bold")
        c.text(W / 2, 430, "Al-Qadr 97:3", 24, GOLD_LIGHT)

    def eid(c, t, p):
        for k in range(14):
            rnd = random.Random(k)
            x = rnd.uniform(100, 1180)
            y = 120 + ((t * rnd.uniform(40, 90) + rnd.uniform(0, 400)) % 420)
            c.circle(x, y, rnd.uniform(5, 10), fill=rnd.choice([GOLD, GOLD_LIGHT, WHITE]))
        c.text(W / 2, 300, "عيد مبارك", 90, GOLD, "arbold")
        c.text(W / 2, 420, "Eid Mubarak", 48, WHITE, "bold")

    return [
        sc("Ramadan är den nionde månaden i den islamiska kalendern. Den börjar när den nya månskäran siktas.", crescent, 8),
        sc("Före gryningen äter man suhur. Profeten ﷺ sa: Ät suhur, för i suhur finns välsignelse.", suhoor, 8),
        sc("Från Fajr till Maghrib avstår man från mat, dryck och allt som bryter fastan, och vaktar sin tunga och sina handlingar.",
           fasting, 9),
        sc("Vid solnedgången bryter man fastan med iftar, gärna med dadlar och vatten enligt sunna.", iftar, 7),
        sc("På kvällarna ber många tarawih tillsammans i moskén och läser Quran.", tarawih, 6),
        sc("Laylat al-Qadr, maktens natt, finns bland de sista tio nätterna. Den är bättre än tusen månader.", qadr, 8),
        sc("Ramadan avslutas med Eid al-Fitr. Zakat al-fitr ska ges före Eid-bönen så att alla kan fira.", eid, 8),
    ]


# ---------------------------------------------------------------------------
# 8. Zakat
# ---------------------------------------------------------------------------

def coins(c, x, y, n, color=GOLD):
    for i in range(n):
        c.ellipse(x - 50, y - i * 12 - 14, x + 50, y - i * 12 + 14, fill=color, outline=(150, 130, 50), width=2)


CATEGORIES = [
    ("🍞", "De fattiga"), ("🏚️", "De behövande"), ("📋", "De som samlar in zakat"), ("🤝", "Nya muslimer"),
    ("🔓", "Att frige fångar"), ("📄", "De skuldsatta"), ("🕌", "På Allahs väg"), ("🧳", "Resande i nöd"),
]


def zakat_scenes():
    def nisab(c, t, p):
        g = seg(p, 0.05, 0.7)
        n = int(2 + g * 14)
        coins(c, 420, 560, n)
        line_y = 560 - 10 * 12 - 14
        c.line([(250, line_y), (650, line_y)], WHITE, 4, round_caps=False)
        c.text(240, line_y, "Nisab", 28, WHITE, "bold", anchor="rm")
        c.text(900, 300, "Nisab", 52, GOLD, "bold")
        c.text(900, 360, "värdet av 85 gram guld", 28, WHITE)
        if n > 10:
            c.text(900, 430, "✓ zakat blir obligatorisk", 26, GOLD_LIGHT)

    def hawl(c, t, p):
        k = seg(p, 0.05, 0.8)
        c.arc(420, 360, 150, -90, -90 + 360 * k, GOLD, 22)
        c.text(420, 360, f"{int(k * 12)}", 80, WHITE, "bold")
        c.text(420, 430, "månmånader", 22, GOLD_LIGHT)
        c.text(900, 320, "Ett helt år", 52, WHITE, "bold")
        c.text(900, 380, "(hawl)", 30, GOLD)

    def percent(c, t, p):
        k = seg(p, 0.1, 0.6)
        cx, cy, r = 420, 360, 170
        c.circle(cx, cy, r, fill=(70, 82, 170))
        c.pie(cx + 18 * k, cy - 18 * k, r, -90, -90 + 9 * k, fill=GOLD)
        c.text(900, 280, "2,5 %", 80, GOLD, "bold")
        c.text(900, 370, "100 000 kr  →  2 500 kr", 32, WHITE, "bold")

    def categories(c, t, p):
        c.text(W / 2, 130, "Åtta grupper som får zakat (9:60)", 34, WHITE, "bold")
        for i, (em, name) in enumerate(CATEGORIES):
            k = seg(p, 0.05 + i * 0.07, 0.15 + i * 0.07)
            x = 250 + (i % 4) * 260
            y = 260 + (i // 4) * 190
            c.emoji(x, y, em, 70, a=k)
            c.text(x, y + 70, name, 22, alpha(GOLD_LIGHT, k), "bold")

    return [
        sc("Zakat är islams tredje pelare: en rättighet som de behövande har i de rikas förmögenhet.",
           lambda c, t, p: (coins(c, 640, 480, 12), c.text(W / 2, 170, "زكاة", 90, GOLD, "arbold"),
                            c.text(W / 2, 260, "Rening av förmögenheten", 32, WHITE, "bold")), 7),
        sc("Zakat är obligatorisk när ditt sparade kapital når nisab, värdet av 85 gram guld.", nisab, 8),
        sc("Kapitalet måste ha legat över nisab under ett helt månår, ett hawl.", hawl, 7),
        sc("Då ger du två och en halv procent. Har du hundratusen kronor blir zakat tvåtusenfemhundra kronor.", percent, 8),
        sc("Koranen nämner åtta grupper som zakat får gå till: de fattiga, de behövande, de som samlar in den, "
           "nya muslimer, att frige fångar, de skuldsatta, på Allahs väg och resande i nöd.", categories, 12),
        sc("Du kan ge din zakat genom Islamiska Förbundet direkt i appen under Donera.",
           lambda c, t, p: (c.emoji(W / 2, 320 + math.sin(t * 2) * 8, "🤲", 150),
                            c.text(W / 2, 470, "Donera → Zakat", 40, GOLD, "bold")), 6),
    ]


# ---------------------------------------------------------------------------
# Catalogue
# ---------------------------------------------------------------------------


def all_videos():
    return [
        Video("pelare", "Islams fem pelare", "Grunden i en muslims liv", pillars_scenes()),
        Video("salah", "Så ber du", "Bönen steg för steg", salah_scenes(), thumb_at=0.4),
        Video("wudu", "Wudu", "Tvagningen steg för steg", wudu_scenes(), thumb_at=0.5),
        Video("tasbih", "Tasbih efter bönen", "33 · 33 · 33 + 1", tasbih_scenes(), thumb_at=0.3),
        Video("bonetider", "Bönetiderna och solen", "Varför tiderna ändras", sun_scenes(), premium=True, thumb_at=0.5),
        Video("hajj", "Hajj steg för steg", "Vallfärden till Mecka", hajj_scenes(), premium=True, thumb_at=0.1),
        Video("ramadan", "Ramadan", "Fastans månad", ramadan_scenes(), premium=True, thumb_at=0.0),
        Video("zakat", "Zakat", "Så fungerar allmosan", zakat_scenes(), premium=True, thumb_at=0.5),
    ]
