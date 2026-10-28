// 生成模板封面 SVG（本地示例素材，无外部依赖）。
// 用法：node scripts/gen-covers.mjs   （输出到 public/demo/covers/）
import { mkdirSync, writeFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const outDir = join(root, "public", "demo", "covers");
mkdirSync(outDir, { recursive: true });

const covers = [
  { id: "travel-postcard", title: "旅行明信片", hue: 200, deco: "mountains" },
  { id: "art-sketch", title: "速写画框", hue: 40, deco: "frame" },
  { id: "festival-greeting", title: "节日贺卡", hue: 340, deco: "sparkles" },
  { id: "love-mosaic", title: "爱心拼图", hue: 0, deco: "heart" },
  { id: "pet-avatar", title: "宠物卡通头像", hue: 120, deco: "paw" },
  { id: "trip-collage", title: "旅行拼贴", hue: 260, deco: "collage" },
];

function deco(kind, accent) {
  switch (kind) {
    case "mountains":
      return `<path d="M0 260 L110 150 L180 230 L260 120 L400 260 Z" fill="${accent}" opacity="0.85"/>
              <circle cx="320" cy="70" r="34" fill="#ffffff" opacity="0.85"/>`;
    case "frame":
      return `<rect x="120" y="70" width="160" height="180" rx="8" fill="none" stroke="${accent}" stroke-width="10"/>
              <rect x="145" y="95" width="110" height="130" rx="4" fill="${accent}" opacity="0.5"/>
              <path d="M155 200 L185 160 L205 185 L230 140 L250 200 Z" fill="${accent}" opacity="0.9"/>`;
    case "sparkles":
      return Array.from({ length: 7 }, (_, i) => {
        const x = 60 + (i * 47) % 280, y = 60 + (i * 71) % 190, r = 8 + (i % 3) * 6;
        return `<path d="M${x} ${y - r} L${x + r * 0.3} ${y - r * 0.3} L${x + r} ${y} L${x + r * 0.3} ${y + r * 0.3} L${x} ${y + r} L${x - r * 0.3} ${y + r * 0.3} L${x - r} ${y} L${x - r * 0.3} ${y - r * 0.3} Z" fill="${accent}" opacity="${0.5 + (i % 3) * 0.2}"/>`;
      }).join("");
    case "heart":
      return `<path d="M200 230 C 120 170 90 120 120 90 C 145 65 185 75 200 105 C 215 75 255 65 280 90 C 310 120 280 170 200 230 Z" fill="${accent}" opacity="0.9"/>
              <circle cx="145" cy="115" r="12" fill="#ffffff" opacity="0.7"/>`;
    case "paw":
      return `<ellipse cx="200" cy="180" rx="52" ry="44" fill="${accent}" opacity="0.9"/>
              <circle cx="150" cy="120" r="17" fill="${accent}" opacity="0.8"/>
              <circle cx="185" cy="100" r="17" fill="${accent}" opacity="0.8"/>
              <circle cx="222" cy="102" r="17" fill="${accent}" opacity="0.8"/>
              <circle cx="252" cy="126" r="16" fill="${accent}" opacity="0.8"/>`;
    case "collage":
      return `<rect x="55" y="60" width="120" height="90" rx="8" fill="${accent}" opacity="0.75"/>
              <rect x="190" y="60" width="150" height="90" rx="8" fill="${accent}" opacity="0.5"/>
              <rect x="55" y="165" width="180" height="85" rx="8" fill="${accent}" opacity="0.6"/>
              <rect x="250" y="165" width="90" height="85" rx="8" fill="${accent}" opacity="0.85"/>`;
    default:
      return "";
  }
}

for (const c of covers) {
  const dark = `hsl(${c.hue} 45% 22%)`;
  const mid = `hsl(${(c.hue + 25) % 360} 42% 30%)`;
  const accent = `hsl(${(c.hue + 70) % 360} 75% 62%)`;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300" viewBox="0 0 400 300" role="img" aria-label="${c.title}">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${dark}"/>
      <stop offset="1" stop-color="${mid}"/>
    </linearGradient>
  </defs>
  <rect width="400" height="300" fill="url(#g)"/>
  ${deco(c.deco, accent)}
  <rect x="0" y="240" width="400" height="60" fill="rgba(0,0,0,0.45)"/>
  <text x="20" y="276" font-family="PingFang SC, Microsoft YaHei, sans-serif" font-size="24" font-weight="700" fill="#f2f2f4">${c.title}</text>
</svg>
`;
  writeFileSync(join(outDir, `${c.id}.svg`), svg);
  console.log("written", `${c.id}.svg`);
}
