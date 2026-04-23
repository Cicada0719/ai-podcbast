import os
from fastapi import FastAPI, BackgroundTasks
from pydantic import BaseModel
from fastapi.staticfiles import StaticFiles
import logging

from netease_api import NeteaseAPI
from llm_engine import LLMEngine
from tts_engine import TTSEngine

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="AI Podcast Engine API", description="Backend for AI DJ Radio & English Learning")

# 挂载静态文件目录供前端获取生成的音频
os.makedirs("static/audio", exist_ok=True)
app.mount("/static", StaticFiles(directory="static"), name="static")

# 实例化服务引擎
netease_client = NeteaseAPI(base_url="http://localhost:3000")
llm_client = LLMEngine()

class GenerateRequest(BaseModel):
    context: str = "早间通勤"
    language_ratio: str = "50% 中文，50% 英文"
    limit_songs: int = 3
    tts_provider: str = "edge" # 支持: edge, aliyun, volcengine, minimax
    podcast_mode: str = "dual" # "single" (单人) 或 "dual" (双人相声)
    theme: str = "随机" # 需求 2: 播客主题
    dj_personality: str = "幽默风趣" # 需求 1: 主播性格
    target_language: str = "English" # 需求 9: 目标语言
    user_message: str = "" # 需求 14: 听众留言

@app.get("/")
async def read_root():
    return {"status": "ok", "message": "AI Podcast Engine is running."}

@app.post("/generate_script")
async def generate_podcast_script(req: GenerateRequest, background_tasks: BackgroundTasks):
    """
    核心工作流：
    1. 从网易云获取每日推荐歌曲
    2. 将歌曲信息喂给 LLM 生成台本
    3. 调用 TTS 生成音频
    """
    logger.info("1. 正在获取网易云每日推荐歌曲...")
    songs = await netease_client.get_daily_recommend_songs(limit=req.limit_songs)
    
    if not songs:
        return {"status": "error", "message": "无法获取网易云歌曲"}

    logger.info(f"成功获取 {len(songs)} 首歌曲, 准备生成 LLM 台本...")
    script_data = await llm_client.generate_dj_script(
        songs_info=songs,
        context=req.context,
        target_language_ratio=req.language_ratio,
        podcast_mode=req.podcast_mode,
        theme=req.theme,
        dj_personality=req.dj_personality,
        target_language=req.target_language,
        user_message=req.user_message
    )
    
    # 动态实例化对应的 TTS 客户端
    tts_client = TTSEngine(provider=req.tts_provider)
    
    # 获取台本后，开始并行或串行生成 TTS 语音
    # 为避免接口等待过长，实际生产中可将 TTS 放入后台任务或使用 WebSocket 推送
    logger.info("3. 台本生成完毕，开始合成 TTS 语音...")
    episodes = script_data.get("episodes", [])
    
    for episode in episodes:
        if episode.get("type") == "dj_talk":
            content = episode.get("content", "")
            speaker = episode.get("speaker", "Echo")
            if content:
                audio_url = await tts_client.generate_audio(content, speaker)
                episode["audio_url"] = audio_url

    logger.info("全部流程处理完毕，返回数据。")
    return {
        "status": "success",
        "data": {
            "context": req.context,
            "songs_count": len(songs),
            "episodes": episodes
        }
    }
