from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="AI Podcast Engine API", description="Backend for AI DJ Radio & English Learning")

@app.get("/")
def read_root():
    return {"status": "ok", "message": "AI Podcast Engine is running."}

@app.post("/generate_script")
def generate_podcast_script():
    # TODO: Connect to LLM and Netease API
    return {"status": "pending", "message": "Script generation endpoint ready"}
