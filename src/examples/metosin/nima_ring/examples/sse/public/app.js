//
// SSE client:
//

console.log("init")

const messages = document.getElementById("messages")
const closeBtn = document.getElementById("close")
const eventSource = new EventSource("/messages")

eventSource.onopen = () => {
  console.log("message: ready")
}

eventSource.onmessage = (e) => {
  console.log("message: data", e)
  messages.textContent = `message: ${e.lastEventId} : ${e.data}`
  console.log("message: data processed")
}

const stateName = {
  [EventSource.CONNECTING]: "CONNECTING",
  [EventSource.CLOSED]: "CLOSED",
  [EventSource.OPEN]: "OPEN",
}

eventSource.onerror = (e) => {
  console.log("message: error", stateName[e.target.readyState], e)
}

closeBtn.onclick = () => {
  console.log("message: close")
  eventSource.close()
}

console.log("loaded")
