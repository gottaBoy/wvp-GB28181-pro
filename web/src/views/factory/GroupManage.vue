<template>
  <div id="groupManage" class="app-container">
    <div class="page-header">
      <h2>厂区分组管理</h2>
      <el-button type="primary" icon="el-icon-plus" @click="handleAdd">添加分组</el-button>
    </div>

    <!-- 厂区选择 -->
    <el-card style="margin-bottom: 20px;">
      <el-form :inline="true" size="small">
        <el-form-item label="选择厂区">
          <el-select
            v-model="selectedApp"
            placeholder="请选择厂区"
            style="width: 200px;"
            @change="loadGroups"
          >
            <el-option
              v-for="factory in factories"
              :key="factory.app"
              :label="factory.name"
              :value="factory.app"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button icon="el-icon-refresh" @click="loadFactories">刷新厂区列表</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 分组列表 -->
    <el-card v-if="selectedApp">
      <div slot="header" class="clearfix">
        <span>分组列表 ({{ groups.length }})</span>
      </div>
      <el-table
        v-loading="loading"
        :data="groups"
        border
        style="width: 100%"
      >
        <el-table-column prop="name" label="分组名称" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="100" show-overflow-tooltip />
        <el-table-column prop="cameraCount" label="摄像头数量" width="120" align="center">
          <template v-slot:default="scope">
            <el-tag type="info">{{ scope.row.cameraCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template v-slot:default="scope">
            <el-button
              size="mini"
              icon="el-icon-edit"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              size="mini"
              icon="el-icon-setting"
              type="primary"
              @click="handleManageProxies(scope.row)"
            >
              管理摄像头
            </el-button>
            <el-button
              size="mini"
              icon="el-icon-delete"
              type="danger"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-else description="请先选择厂区" />

    <!-- 添加/编辑分组对话框 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="500px"
      @close="resetForm"
    >
      <el-form
        ref="groupForm"
        :model="groupForm"
        :rules="groupRules"
        label-width="100px"
      >
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="请输入分组名称，如：退洗库、黑皮库" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="groupForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分组描述"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number
            v-model="groupForm.sortOrder"
            :min="0"
            :max="9999"
            placeholder="数字越小越靠前"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </div>
    </el-dialog>

    <!-- 管理摄像头对话框 -->
    <el-dialog
      title="管理分组摄像头"
      :visible.sync="proxyDialogVisible"
      width="700px"
      @close="resetProxyForm"
    >
      <div v-if="currentGroup">
        <el-alert
          :title="`分组：${currentGroup.name}`"
          type="info"
          :closable="false"
          style="margin-bottom: 10px;"
        />
        
        <!-- 添加摄像头 -->
        <el-card style="margin-bottom: 10px;" :body-style="{ padding: '10px' }">
          <div slot="header" style="padding: 5px 0;">
            <span style="font-size: 14px;">添加摄像头到分组</span>
          </div>
          <el-form :inline="true" size="small" style="margin: 0;">
            <el-form-item label="应用名" style="margin-bottom: 5px;">
              <el-input v-model="proxyForm.app" placeholder="应用名" style="width: 120px;" />
            </el-form-item>
            <el-form-item label="流ID" style="margin-bottom: 5px;">
              <el-input v-model="proxyForm.stream" placeholder="流ID" style="width: 120px;" />
            </el-form-item>
            <el-form-item style="margin-bottom: 5px;">
              <el-button type="primary" size="small" @click="handleAddProxy">添加</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 批量添加 -->
        <el-card style="margin-bottom: 10px;" :body-style="{ padding: '10px' }">
          <div slot="header" style="padding: 5px 0;">
            <span style="font-size: 14px;">批量添加摄像头</span>
          </div>
          <el-input
            v-model="batchProxiesText"
            type="textarea"
            :rows="3"
            placeholder="每行一个，格式：app,stream&#10;例如：&#10;guangqing,201002&#10;guangqing,201003"
            style="margin-bottom: 8px;"
          />
          <el-button type="primary" size="small" @click="handleBatchAddProxies">批量添加</el-button>
        </el-card>

        <!-- 分组下的摄像头列表 -->
        <el-card :body-style="{ padding: '10px' }">
          <div slot="header" style="padding: 5px 0;">
            <span style="font-size: 14px;">分组下的摄像头 ({{ groupProxies.length }})</span>
          </div>
          <el-table
            v-loading="proxyLoading"
            :data="groupProxies"
            border
            size="small"
            max-height="250"
          >
            <el-table-column prop="app" label="应用名" width="120" />
            <el-table-column prop="stream" label="流ID" width="120" />
            <el-table-column label="操作" width="80" fixed="right">
              <template v-slot:default="scope">
                <el-button
                  size="mini"
                  type="danger"
                  icon="el-icon-delete"
                  @click="handleRemoveProxy(scope.row)"
                >
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getFactoryListSummary,
  getFactoryGroups,
  addFactoryGroup,
  updateFactoryGroup,
  deleteFactoryGroup,
  addProxyToGroup,
  removeProxyFromGroup,
  batchAddProxyToGroup,
  getFactoryCameras
} from '@/api/factory'

export default {
  name: 'GroupManage',
  data() {
    return {
      factories: [],
      selectedApp: '',
      groups: [],
      loading: false,
      dialogVisible: false,
      dialogTitle: '添加分组',
      groupForm: {
        id: null,
        name: '',
        description: '',
        app: '',
        sortOrder: 0
      },
      groupRules: {
        name: [
          { required: true, message: '请输入分组名称', trigger: 'blur' },
          { max: 255, message: '分组名称长度不能超过255个字符', trigger: 'blur' }
        ]
      },
      saving: false,
      proxyDialogVisible: false,
      currentGroup: null,
      groupProxies: [],
      proxyLoading: false,
      proxyForm: {
        app: '',
        stream: ''
      },
      batchProxiesText: ''
    }
  },
  created() {
    this.loadFactories()
  },
  methods: {
    // 加载厂区列表
    async loadFactories() {
      try {
        const response = await getFactoryListSummary()
        this.factories = response.data || []
        if (this.factories.length > 0 && !this.selectedApp) {
          this.selectedApp = this.factories[0].app
          this.loadGroups()
        }
      } catch (error) {
        this.$message.error('加载厂区列表失败: ' + (error.message || '未知错误'))
      }
    },

    // 加载分组列表
    async loadGroups() {
      if (!this.selectedApp) {
        this.groups = []
        return
      }

      this.loading = true
      try {
        const response = await getFactoryGroups(this.selectedApp)
        this.groups = response.data || []
      } catch (error) {
        this.$message.error('加载分组列表失败: ' + (error.message || '未知错误'))
      } finally {
        this.loading = false
      }
    },

    // 添加分组
    handleAdd() {
      if (!this.selectedApp) {
        this.$message.warning('请先选择厂区')
        return
      }
      this.dialogTitle = '添加分组'
      this.groupForm = {
        id: null,
        name: '',
        description: '',
        app: this.selectedApp,
        sortOrder: 0
      }
      this.dialogVisible = true
    },

    // 编辑分组
    handleEdit(row) {
      this.dialogTitle = '编辑分组'
      this.groupForm = {
        id: row.id,
        name: row.name,
        description: row.description || '',
        app: row.app,
        sortOrder: row.sortOrder || 0
      }
      this.dialogVisible = true
    },

    // 保存分组
    async handleSave() {
      this.$refs.groupForm.validate(async (valid) => {
        if (!valid) return

        this.saving = true
        try {
          if (this.groupForm.id) {
            // 更新
            await updateFactoryGroup(this.groupForm)
            this.$message.success('更新分组成功')
          } else {
            // 添加
            await addFactoryGroup(this.groupForm)
            this.$message.success('添加分组成功')
          }
          this.dialogVisible = false
          this.loadGroups()
        } catch (error) {
          this.$message.error((error.response?.data?.msg || error.message) || '操作失败')
        } finally {
          this.saving = false
        }
      })
    },

    // 删除分组
    handleDelete(row) {
      this.$confirm(`确定要删除分组"${row.name}"吗？删除后该分组下的所有摄像头关联将被移除。`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteFactoryGroup(row.id)
          this.$message.success('删除分组成功')
          this.loadGroups()
        } catch (error) {
          this.$message.error((error.response?.data?.msg || error.message) || '删除失败')
        }
      }).catch(() => {})
    },

    // 管理摄像头
    async handleManageProxies(row) {
      this.currentGroup = row
      this.proxyForm.app = row.app
      this.proxyDialogVisible = true
      await this.loadGroupProxies(row.id)
    },

    // 加载分组下的摄像头
    async loadGroupProxies(groupId) {
      this.proxyLoading = true
      try {
        // 通过获取该厂区的所有摄像头，然后筛选出属于该分组的
        const response = await getFactoryCameras(this.currentGroup.app, false)
        const allCameras = response.data || []
        
        // 筛选出属于当前分组的摄像头
        this.groupProxies = allCameras
          .filter(camera => camera.groupId === groupId)
          .map(camera => ({
            app: camera.app,
            stream: camera.stream,
            name: camera.name
          }))
      } catch (error) {
        this.$message.error('加载摄像头列表失败: ' + (error.message || '未知错误'))
      } finally {
        this.proxyLoading = false
      }
    },

    // 添加摄像头到分组
    async handleAddProxy() {
      if (!this.proxyForm.app || !this.proxyForm.stream) {
        this.$message.warning('请输入应用名和流ID')
        return
      }

      try {
        await addProxyToGroup(this.currentGroup.id, this.proxyForm.app, this.proxyForm.stream)
        this.$message.success('添加摄像头成功')
        this.proxyForm.stream = '' // 只清空流ID，保留app
        await this.loadGroupProxies(this.currentGroup.id)
        this.loadGroups() // 刷新分组列表以更新摄像头数量
      } catch (error) {
        this.$message.error((error.response?.data?.msg || error.message) || '添加失败')
      }
    },

    // 批量添加摄像头
    async handleBatchAddProxies() {
      if (!this.batchProxiesText.trim()) {
        this.$message.warning('请输入要添加的摄像头列表')
        return
      }

      const lines = this.batchProxiesText.trim().split('\n').filter(line => line.trim())
      const proxies = []

      for (const line of lines) {
        const parts = line.split(',').map(s => s.trim())
        if (parts.length >= 2) {
          proxies.push({
            app: parts[0],
            stream: parts[1]
          })
        }
      }

      if (proxies.length === 0) {
        this.$message.warning('没有有效的摄像头数据，格式：app,stream')
        return
      }

      try {
        const response = await batchAddProxyToGroup(this.currentGroup.id, proxies)
        const result = response.data
        this.$message.success(`批量添加完成：成功 ${result.successCount} 个，失败 ${result.failCount} 个`)
        if (result.errors && result.errors.length > 0) {
          console.error('批量添加错误:', result.errors)
        }
        this.batchProxiesText = ''
        await this.loadGroupProxies(this.currentGroup.id)
        this.loadGroups() // 刷新分组列表
      } catch (error) {
        this.$message.error((error.response?.data?.msg || error.message) || '批量添加失败')
      }
    },

    // 移除摄像头
    handleRemoveProxy(row) {
      this.$confirm(`确定要从分组中移除摄像头 ${row.app}/${row.stream} 吗？`, '确认移除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await removeProxyFromGroup(this.currentGroup.id, row.app, row.stream)
          this.$message.success('移除摄像头成功')
          await this.loadGroupProxies(this.currentGroup.id)
          this.loadGroups() // 刷新分组列表
        } catch (error) {
          this.$message.error((error.response?.data?.msg || error.message) || '移除失败')
        }
      }).catch(() => {})
    },

    // 重置表单
    resetForm() {
      this.$refs.groupForm?.resetFields()
      this.groupForm = {
        id: null,
        name: '',
        description: '',
        app: '',
        sortOrder: 0
      }
    },

    // 重置摄像头管理表单
    resetProxyForm() {
      this.currentGroup = null
      this.groupProxies = []
      this.proxyForm = {
        app: '',
        stream: ''
      }
      this.batchProxiesText = ''
    }
  }
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
  }
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}

.clearfix:after {
  clear: both;
}
</style>

