import edge_tts
import asyncio
import os
import uuid
import logging
import httpx
import json

logger = logging.getLogger(__name__)

class TTSEngine:
    def __init__(self, provider: str = "edge", voice: str = "zh-CN-XiaoxiaoNeural"):
        """
        初始化 TTS 引擎，支持多种 Provider
        :param provider: "edge", "minimax", "aliyun", "volcengine"
        """
        self.provider = provider
        self.voice = voice
        self.output_dir = os.path.join(os.path.dirname(__file__), "static", "audio")
        os.makedirs(self.output_dir, exist_ok=True)

    async def generate_audio(self, text: str, speaker_name: str = "Echo") -> str:
        """
        将文本转换为音频并保存，返回文件路径或 URL
        """
        filename = f"{uuid.uuid4().hex[:8]}_{speaker_name}.mp3"
        output_path = os.path.join(self.output_dir, filename)

        try:
            logger.info(f"开始使用 [{self.provider}] 生成 TTS 音频: {text[:20]}...")
            
            if self.provider == "edge":
                await self._generate_edge(text, output_path)
            elif self.provider == "minimax":
                await self._generate_minimax(text, output_path)
            elif self.provider == "aliyun":
                await self._generate_aliyun(text, output_path)
            elif self.provider == "volcengine":
                await self._generate_volcengine(text, output_path)
            else:
                logger.error(f"不支持的 TTS provider: {self.provider}，回退至 edge")
                await self._generate_edge(text, output_path)

            logger.info(f"音频保存成功: {output_path}")
            return f"/static/audio/{filename}"
            
        except Exception as e:
            logger.error(f"TTS 生成失败: {e}")
            return ""

    async def _generate_edge(self, text: str, output_path: str):
        communicate = edge_tts.Communicate(text, self.voice)
        await communicate.save(output_path)

    async def _generate_minimax(self, text: str, output_path: str):
        # 预留 MiniMax T2A 接口调用示例
        api_key = os.getenv("MINIMAX_API_KEY")
        group_id = os.getenv("MINIMAX_GROUP_ID")
        if not api_key:
            logger.warning("未配置 MINIMAX_API_KEY，回退至 edge")
            return await self._generate_edge(text, output_path)
            
        url = f"https://api.minimax.chat/v1/t2a_v2?GroupId={group_id}"
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
        payload = {
            "model": "speech-01-turbo",
            "text": text,
            "stream": False,
            "voice_setting": {
                "voice_id": "male-qn-qingse", # 可配置
                "speed": 1,
                "vol": 1,
                "pitch": 0
            }
        }
        async with httpx.AsyncClient() as client:
            resp = await client.post(url, headers=headers, json=payload, timeout=60.0)
            if resp.status_code == 200:
                data = resp.json()
                # 假设 MiniMax 返回的是音频文件的二进制下载链接或直接二进制
                # 这里做简化处理，实际需根据官方最新文档解析
                pass

    async def _generate_aliyun(self, text: str, output_path: str):
        # 预留阿里云 TTS 接口
        logger.info("阿里云 TTS 接口占位")
        # 实现阿里云 OpenAPI Token 鉴权并请求
        await self._generate_edge(text, output_path)

    async def _generate_volcengine(self, text: str, output_path: str):
        # 预留火山引擎 TTS 接口
        logger.info("火山引擎 TTS 接口占位")
        # 实现火山引擎鉴权与请求
        await self._generate_edge(text, output_path)
