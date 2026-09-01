import{afterEach,describe,expect,it,vi}from'vitest'
import{dormitoryApi}from'./dormitory'

describe('dormitoryApi',()=>{
 afterEach(()=>vi.unstubAllGlobals())
 it('loads the employee dormitory resource tree with the login token',async()=>{
  vi.stubGlobal('sessionStorage',{getItem:(key:string)=>key==='visitor-authorization'?'Bearer test-token':null})
  const fetchMock=vi.fn().mockResolvedValue({ok:true,status:200,json:async()=>({buildings:[]})})
  vi.stubGlobal('fetch',fetchMock)
  await expect(dormitoryApi.tree()).resolves.toEqual({buildings:[]})
  expect(fetchMock).toHaveBeenCalledWith('/api/visitor/dormitory/employee/resources/tree',expect.objectContaining({headers:expect.objectContaining({Authorization:'Bearer test-token'})}))
 })
})
