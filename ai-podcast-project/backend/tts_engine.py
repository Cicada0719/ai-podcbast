import edge_tts
import asyncio
import os
import uuid
import logging

logger = logging.getLogger(__name__)

class TTSEngine:
    def __init__(self, voice: str = "zh-CN-XiaoxiaoNeural"):
        """
        初始化 Edge TTS 引擎
        支持的优秀双语发音人:
        - zh-CN-XiaoxiaoNeural (温柔女声)
        - zh-CN-YunxiNeural (阳光男声)
        - en-US-AriaNeural (美式女声)
        """
        self.voice = voice
        self.output_dir = os.path.join(os.path.dirname(__file__), "static", "audio")
        os.makedirs(self.output_dir, exist_ok=True)

    async def generate_audio(self, text: str, speaker_name: str = "Echo") -> str:
        """
        将文本转换为音频并保存，返回文件路径或 URL
        """
        # 随机生成音频文件名
        filename = f"{uuid.uuid4().hex[:8]}_{speaker_name}.mp3"
        output_path = os.path.join(self.output_dir, filename)

        try:
            logger.info(f"开始生成 TTS 音频: {text[:20]}...")
            communicate = edge_tts.Communicate(text, self.voice)
            await communicate.save(output_path)
            logger.info(f"音频保存成功: {output_path}")
            
            # 返回相对路径以便于前端拉取 (如 /static/audio/xxx.mp3)
            return f"/static/audio/{filename}"
            
        except Exception as e:
            logger.error(f"TTS 生成失败: {e}")
            return ""

    def get_available_voices(self):
        """获取支持的声音列表"""
        # 此处可拓展返回支持的双人/多人发音列表
        return ["zh-CN-XiaoxiaoNeural", "zh-CN-YunxiNeural", "en-US-AriaNeural", "en-US-GuyNeural"]
