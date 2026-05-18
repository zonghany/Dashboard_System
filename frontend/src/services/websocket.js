import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

class WebSocketService {
  constructor() {
    this.client = null
    this.subscriptions = {}
  }

  connect(projectId, onMessage) {
    this.disconnect()

    const token = localStorage.getItem('token')
    const sock = new SockJS(`/ws?token=${token}`)

    this.client = new Client({
      webSocketFactory: () => sock,
      reconnectDelay: 5000,
      debug: false,
      onConnect: () => {
        const sub = this.client.subscribe(`/topic/board/${projectId}`, msg => {
          try {
            const data = JSON.parse(msg.body)
            onMessage(data)
          } catch (e) {
            console.error('WS message parse error:', e)
          }
        })
        this.subscriptions[`board_${projectId}`] = sub

        const userSub = this.client.subscribe(`/topic/user/${this.getUserId()}`, msg => {
          try {
            const data = JSON.parse(msg.body)
            onMessage(data)
          } catch (e) {
            console.error('WS notification parse error:', e)
          }
        })
        this.subscriptions[`user_${this.getUserId()}`] = userSub
      }
    })

    this.client.activate()
  }

  getUserId() {
    try {
      const token = localStorage.getItem('token')
      if (!token) return null
      const payload = JSON.parse(atob(token.split('.')[1]))
      return payload.userId
    } catch {
      return null
    }
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate()
      this.client = null
      this.subscriptions = {}
    }
  }

  send(destination, body) {
    if (this.client && this.client.connected) {
      this.client.publish({ destination, body: JSON.stringify(body) })
    }
  }
}

export default new WebSocketService()