package com.ytx.ai.base.util;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.util.Pool;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * 对象工具类
 * 提供基于 Kryo 的深度克隆功能
 *
 * @author ytx
 */
public class ObjectUtils {

    /**
     * Kryo 对象池
     * Pool 是线程安全的,可以在多线程环境共享
     * maximumCapacity: 最大容量为 20
     * softReferences: 不使用软引用
     */
    private static final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 20) {
        /**
         * 创建并配置 Kryo 实例
         * 当对象池需要新实例时调用此方法
         *
         * @return 配置好的 Kryo 实例
         */
        @Override
        protected Kryo create() {
            Kryo kryo = new Kryo();
            // --- 配置开始 ---
            // 允许未注册的类(一般业务场景建议 false,追求极致性能设为 true 并手动注册)
            kryo.setRegistrationRequired(false);
            // 开启引用检测(解决循环引用问题,克隆必须开启)
            kryo.setReferences(true);
            // 配置实例化策略:如果类没有无参构造函数,尝试使用 Objenesis 库实例化
            // Kryo.DefaultInstantiatorStrategy 是回退策略
            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(
                    new StdInstantiatorStrategy()
            ));
            // --- 配置结束 ---
            return kryo;
        }
    };

    /**
     * 深度克隆方法
     * 使用 Kryo 序列化框架实现对象的深度拷贝
     * 支持循环引用和复杂对象结构的克隆
     *
     * @param original 源对象，需要被克隆的对象
     * @param <T> 泛型类型，与源对象类型保持一致
     * @return 克隆后的新对象，如果源对象为 null 则返回 null
     */
    public static <T> T deepClone(T original) {
        // 参数校验：如果源对象为 null，直接返回 null
        if (original == null) {
            return null;
        }

        // 从池中获取 Kryo 实例
        Kryo kryo = kryoPool.obtain();
        try {
            // 使用 kryo.copy() 进行内存深拷贝
            // 注意:不要使用 writeObject/readObject,copy 方法针对内存拷贝做了优化
            return kryo.copy(original);
        } finally {
            // 使用完毕后归还给池，确保资源正确释放
            kryoPool.free(kryo);
        }
    }
}

