Vue.component("footBar", {
  template: `
    <div class="foot">
    <div class="foot-box" :class="{active: activeBtn === 1}" @click="toPage(1)">
      <div class="foot-view"><i class="el-icon-s-home"></i></div>
      <div class="foot-text">首页</div>
    </div>
    <div class="foot-box" :class="{active: activeBtn === 2}" @click="toPage(2)">
      <div class="foot-view"><i class="el-icon-map-location"></i></div>
      <div class="foot-text">地图</div>
    </div>
    <div class="foot-box" @click="toPage(0)">
      <img class="add-btn" src="/imgs/add.png" alt="">
    </div>
    <div class="foot-box msg-box" :class="{active: activeBtn === 3}" @click="toPage(3)">
      <div class="foot-view"><i class="el-icon-chat-dot-round"></i></div>
      <div class="foot-text">消息</div>
      <span class="msg-badge" v-if="unreadCount > 0">{{unreadCount > 99 ? '99+' : unreadCount}}</span>
    </div>
    <div class="foot-box" :class="{active: activeBtn === 4}" @click="toPage(4)">
      <div class="foot-view"><i class="el-icon-user"></i></div>
      <div class="foot-text">我的</div>
    </div>
  </div>
  `,
  data() {
    return {
      unreadCount: 0
    }
  },
  props: ['activeBtn'],
  created() {
    this.fetchUnread();
    // 每30秒检查一次未读消息
    this._timer = setInterval(() => this.fetchUnread(), 30000);
  },
  beforeDestroy() {
    if (this._timer) clearInterval(this._timer);
  },
  methods: {
    fetchUnread() {
      let token = sessionStorage.getItem("token");
      if (!token) return;
      axios.get("/message/unread-count")
        .then(({data}) => { this.unreadCount = data || 0; })
        .catch(() => {});
    },
    toPage(i) {
      if (i === 0) {
        // 发布：弹出选择菜单（发布商品 / 发晒物）
        if (window.XtPublish && XtPublish.show) {
          XtPublish.show();
        } else {
          location.href = "/note-edit.html"
        }
      } else if (i === 2) {
        // 地图/附近：查看全部商品（按距离排序）
        location.href = "/goods-list.html?type=0&name=全部"
      } else if (i === 4) {
        location.href = "/info.html"
      } else if (i === 3) {
        location.href = "/message.html"
      } else if (i === 1){
        location.href = "/"
      }
    }
  }
})