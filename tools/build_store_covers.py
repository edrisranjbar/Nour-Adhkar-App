from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools" / "vendor"))

import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont


OUT = ROOT / "store-assets" / "covers"
FONT_BOLD = ROOT / "app" / "src" / "main" / "res" / "font" / "vazirmatn_bold.ttf"
FONT_REGULAR = ROOT / "app" / "src" / "main" / "res" / "font" / "vazirmatn_regular.ttf"


def crop_feature(source: Path) -> Image.Image:
    image = Image.open(source).convert("RGB")
    target_ratio = 1024 / 500
    width, height = image.size
    crop_height = round(width / target_ratio)
    top = max(0, (height - crop_height) // 2)
    return image.crop((0, top, width, top + crop_height)).resize((1024, 500), Image.Resampling.LANCZOS)


def rtl_text(draw: ImageDraw.ImageDraw, xy, text, font, fill, anchor="ra", **kwargs):
    shaped = get_display(arabic_reshaper.reshape(text))
    draw.text(xy, shaped, font=font, fill=fill, anchor=anchor, **kwargs)


def cover_one(source: Path, destination: Path):
    image = crop_feature(source)
    overlay = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    # A soft tonal veil protects readability while preserving the generated scene.
    draw.rounded_rectangle((38, 72, 524, 428), radius=34, fill=(247, 250, 243, 226), outline=(255, 255, 255, 120), width=2)
    draw.rounded_rectangle((397, 105, 484, 151), radius=23, fill=(58, 105, 49, 255))
    rtl_text(draw, (465, 128), "اذکار نور", ImageFont.truetype(FONT_BOLD, 22), "white", anchor="rm")
    rtl_text(draw, (468, 219), "آرامش روزانه", ImageFont.truetype(FONT_BOLD, 51), "#24472B")
    rtl_text(draw, (468, 281), "با یاد خدا", ImageFont.truetype(FONT_BOLD, 51), "#24472B")
    rtl_text(draw, (468, 344), "اذکار صبحگاه و شامگاه، صوت و تسبیح", ImageFont.truetype(FONT_REGULAR, 23), "#43493F")
    draw.rounded_rectangle((408, 377, 468, 383), radius=3, fill=(216, 181, 106, 255))
    Image.alpha_composite(image.convert("RGBA"), overlay).convert("RGB").save(destination, "PNG", optimize=True)


def cover_two(source: Path, destination: Path):
    image = crop_feature(source)
    overlay = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    draw.rounded_rectangle((42, 74, 492, 426), radius=34, fill=(247, 250, 243, 224), outline=(255, 255, 255, 110), width=2)
    draw.rounded_rectangle((359, 105, 452, 151), radius=23, fill=(36, 71, 43, 255))
    rtl_text(draw, (433, 128), "اذکار نور", ImageFont.truetype(FONT_BOLD, 22), "white", anchor="rm")
    rtl_text(draw, (438, 219), "همراه هر روزِ", ImageFont.truetype(FONT_BOLD, 49), "#24472B")
    rtl_text(draw, (438, 279), "ذکر شما", ImageFont.truetype(FONT_BOLD, 49), "#24472B")
    rtl_text(draw, (438, 340), "یادآور، پخش صوتی و تسبیح در یک‌جا", ImageFont.truetype(FONT_REGULAR, 23), "#43493F")
    draw.rounded_rectangle((378, 377, 438, 383), radius=3, fill=(216, 181, 106, 255))
    Image.alpha_composite(image.convert("RGBA"), overlay).convert("RGB").save(destination, "PNG", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    generated = Path(r"C:\Users\edi\.codex\generated_images\019fe643-4c6f-76b0-aa99-eb0ca0d8ed6f")
    cover_one(generated / "exec-683fd170-11f4-419e-9bf5-7f2597ad74fe.png", OUT / "nour-adhkar-cover-01-serene-dawn.png")
    cover_two(generated / "exec-f2bcac63-803b-4605-96f3-6c9f73332f16.png", OUT / "nour-adhkar-cover-02-daily-companion.png")
    for path in sorted(OUT.glob("*.png")):
        with Image.open(path) as image:
            print(f"{path}: {image.size[0]}x{image.size[1]} {image.mode} {path.stat().st_size} bytes")


if __name__ == "__main__":
    main()
